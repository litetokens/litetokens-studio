package org.litetokens.abi.datatypes.generated;

import java.util.List;
import org.litetokens.abi.datatypes.StaticArray;
import org.litetokens.abi.datatypes.Type;

/**
 * Auto generated code.
 * <p><strong>Do not modifiy!</strong>
 * <p>Please use org.web3j.codegen.AbiTypesGenerator in the 
 * <a href="https://github.com/web3j/web3j/tree/master/codegen">codegen module</a> to update.
 */
public class StaticArray2<T extends Type> extends StaticArray<T> {
    public StaticArray2(List<T> values) {
        super(2, values);
    }

    @SafeVarargs
    public StaticArray2(T... values) {
        super(2, values);
    }
}
