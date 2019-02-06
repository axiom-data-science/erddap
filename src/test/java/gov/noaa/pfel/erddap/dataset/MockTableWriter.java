package gov.noaa.pfel.erddap.dataset;

import gov.noaa.pfel.coastwatch.pointdata.Table;

public class MockTableWriter extends TableWriter {

    public Table table;

    public MockTableWriter(EDD tEdd, String tNewHistory, OutputStreamSource tOutputStreamSource) {
        super(tEdd, tNewHistory, tOutputStreamSource);
    }

    @Override
    public void writeSome(Table table) throws Throwable {
        this.table = table;
    }

    @Override
    public void finish() throws Throwable {
    }
}
