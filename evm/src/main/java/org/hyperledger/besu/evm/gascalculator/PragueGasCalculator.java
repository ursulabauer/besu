/*
 * Copyright contributors to Hyperledger Besu.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.besu.evm.gascalculator;

import static org.hyperledger.besu.datatypes.Address.BLS12_MAP_FP2_TO_G2;
import static org.hyperledger.besu.evm.internal.Words.clampedAdd;

import org.hyperledger.besu.datatypes.CodeDelegation;

import org.apache.tuweni.bytes.Bytes;

/**
 * Gas Calculator for Prague
 *
 * <UL>
 *   <LI>Gas costs for EIP-7702 (Code Delegation)
 * </UL>
 */
public class PragueGasCalculator extends CancunGasCalculator {
  private static final long TOTAL_COST_FLOOR_PER_TOKEN = 10L;

  final long existingAccountGasRefund;

  /** Instantiates a new Prague Gas Calculator. */
  public PragueGasCalculator() {
    this(BLS12_MAP_FP2_TO_G2.toArrayUnsafe()[19]);
  }

  /**
   * Instantiates a new Prague Gas Calculator
   *
   * @param maxPrecompile the max precompile
   */
  protected PragueGasCalculator(final int maxPrecompile) {
    super(maxPrecompile);
    this.existingAccountGasRefund = newAccountGasCost() - CodeDelegation.PER_AUTH_BASE_COST;
  }

  @Override
  public long delegateCodeGasCost(final int delegateCodeListLength) {
    return newAccountGasCost() * delegateCodeListLength;
  }

  @Override
  public long calculateDelegateCodeGasRefund(final long alreadyExistingAccounts) {
    return existingAccountGasRefund * alreadyExistingAccounts;
  }

  @Override
  public long transactionIntrinsicGasCost(
      final Bytes payload, final boolean isContractCreation, final long baselineGas) {
    final long dynamicIntrinsicGasCost =
        dynamicIntrinsicGasCost(payload, isContractCreation, baselineGas);
    final long totalCostFloor =
        tokensInCallData(payload.size(), zeroBytes(payload)) * TOTAL_COST_FLOOR_PER_TOKEN;

    return clampedAdd(TX_BASE_COST, Math.max(dynamicIntrinsicGasCost, totalCostFloor));
  }

  private long tokensInCallData(final long payloadSize, final long zeroBytes) {
    // as defined in https://eips.ethereum.org/EIPS/eip-7623#specification
    return clampedAdd(zeroBytes, (payloadSize - zeroBytes) * 4);
  }
}
