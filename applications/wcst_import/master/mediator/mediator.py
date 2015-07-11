from master.generator.model.bounded_by import BoundedBy
from master.generator.model.domain_set_irregular import DomainSetIrregular
from master.generator.model.domain_set_regular import DomainSetRegular
from master.generator.model.range_set_file import RangeSetFile
from master.generator.model.range_set_tuple_list import RangeSetTupleList
from master.generator.model.range_type import RangeType
from master.generator.model.rectified_grid_coverage import RectifiedGridCoverage
from master.generator.model.referenceable_grid_coverage import ReferenceableGridCoverage
from master.provider.data.file_data_provider import FileDataProvider
from master.provider.data.tuple_list_data_provider import TupleListDataProvider
from master.provider.metadata.metadata_provider import MetadataProvider
from util.file_obj import File
from util.file_util import FileUtil


class Mediator:
    def __init__(self, metadata_provider, data_provider):
        """
        This class mediates the providers with the gml model
        :param MetadataProvider metadata_provider: the metadata provider
        :param DataProvider data_provider: the data provider
        :return:
        """
        self.metadata_provider = metadata_provider
        self.data_provider = data_provider

    def get_gml_coverage(self):
        """
        Returns a coverage model that can be transformed into a gml
        :rtype: GMLCoverage
        """
        if self.metadata_provider.is_coverage_irregular():
            return ReferenceableGridCoverage(self.metadata_provider.coverage_id, self._get_bounded_by(),
                                             self._get_domain_set(), self._get_range_set(), self._get_range_type())
        else:
            return RectifiedGridCoverage(self.metadata_provider.coverage_id, self._get_bounded_by(),
                                         self._get_domain_set(), self._get_range_set(), self._get_range_type())

    def get_gml_file(self):
        """
        Returns the file path to the file containing the coverage held by the mediator
        :rtype: File
        """
        gml = self.get_gml_coverage().to_gml()
        fu = FileUtil()
        return File(fu.write_to_tmp_file(gml))

    def _get_bounded_by(self):
        mp = self.metadata_provider
        return BoundedBy(mp.get_crs(), mp.get_axis_labels(), mp.get_axis_uom_labels(),
            mp.get_no_of_dimensions(), mp.get_lower_corner(), mp.get_upper_corner())

    def _get_domain_set(self):
        mp = self.metadata_provider
        if mp.is_coverage_irregular():
            return DomainSetIrregular(mp.get_no_of_dimensions(), mp.get_grid_low(), mp.get_grid_high(),
                mp.get_axis_labels_grid(), mp.get_axis_labels(), mp.get_crs(), mp.get_axis_uom_labels(),
                mp.get_origin(), mp.get_offset_vectors())
        else:
            return DomainSetRegular(mp.get_no_of_dimensions(), mp.get_grid_low(), mp.get_grid_high(),
                mp.get_axis_labels_grid(), mp.get_axis_labels(), mp.get_crs(), mp.get_axis_uom_labels(),
                mp.get_origin(), mp.get_offset_vectors())

    def _get_range_set(self):
        dp = self.data_provider
        if isinstance(dp, FileDataProvider):
            return RangeSetFile(dp.get_file_path(), dp.get_mimetype())
        elif isinstance(dp, TupleListDataProvider):
            return RangeSetTupleList(dp.get_tuple_list())

    def _get_range_type(self):
        return RangeType(self.metadata_provider.get_range_fields())