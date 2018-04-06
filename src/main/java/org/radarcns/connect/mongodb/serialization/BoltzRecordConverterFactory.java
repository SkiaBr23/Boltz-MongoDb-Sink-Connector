package org.radarcns.connect.mongodb.serialization;
/**
 * Extended RecordConverterFactory to allow customized RecordConverter class that are needed
 */

import java.util.List;
import java.util.ArrayList;

public class BoltzRecordConverterFactory extends RecordConverterFactory {

    /**
     * Overrides genericConverter to append custom RecordConverter class to RecordConverterFactory
     *
     * @return list of RecordConverters available
     */
    protected List<RecordConverter> genericConverters() {
        List<RecordConverter> recordConverters = new ArrayList<RecordConverter>();
        recordConverters.addAll(super.genericConverters());
        recordConverters.add(new BoltzRecordConverter());
        return recordConverters;
    }

}