import json
import os
from config_manager import ConfigManager
from master.error.runtime_exception import RuntimeException
from master.importer.slice import Slice
from master.provider.data.data_provider import DataProvider
from util.log import log


class Resumer:
    def __init__(self, coverage_id):
        """
        The resumer keeps track of data providers that have been imported so that a record is kept if several
        runs are performed
        :param str coverage_id: the id of the coverage that is imported
        """
        self.__RESUMER_FILE_NAME__ = ConfigManager.resumer_dir_path + coverage_id + self.__RESUMER_FILE_NAME__
        self.imported_data = []
        try:
            if os.path.isfile(self.__RESUMER_FILE_NAME__) and os.access(self.__RESUMER_FILE_NAME__, os.R_OK):
                log.info(
                    "We found a resumer file in the ingredients folder. The slices listed there will not be imported.")
                self.resume_fp = open(self.__RESUMER_FILE_NAME__)
                self.imported_data = json.loads(self.resume_fp.read())
                self.resume_fp.close()
        except IOError as e:
            raise RuntimeException("Could not read the resume file, full error message: " + str(e))
        except ValueError as e:
            log.warn("The resumer JSON file could not be parsed. A new one will be created.")

    def checkpoint(self):
        """
        Adds a checkpoint and saves to the backing file
        """
        self.resume_fp = open(self.__RESUMER_FILE_NAME__, "w")
        json.dump(self.imported_data, self.resume_fp)
        self.resume_fp.close()

    def add_imported_data(self, data_provider):
        """
        Adds a data provider to the list of imported data
        :param DataProvider data_provider: The data provider that was imported
        """
        self.imported_data.append(data_provider.to_eq_hash())
        self.checkpoint()

    def eliminate_already_imported_slices(self, slices):
        """
        Eliminates the slices that were already imported and returns a new array of slices
        :param list[Slice] slices: a list of slices
        :rtype: list[Slice]
        """
        ret_slices = []
        for slice in slices:
            if slice.data_provider.to_eq_hash() not in self.imported_data:
                ret_slices.append(slice)
        return ret_slices


    __RESUMER_FILE_NAME__ = ".resume.json"