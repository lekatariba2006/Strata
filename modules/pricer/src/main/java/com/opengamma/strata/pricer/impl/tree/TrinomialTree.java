/*
 * Copyright (C) 2016 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.pricer.impl.tree;

import com.opengamma.strata.basics.value.ValueDerivatives;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.array.DoubleArray;
import com.opengamma.strata.pricer.fxopt.RecombiningTrinomialTreeData;

/**
 * Trinomial tree.
 * <p>
 * Option pricing model based on trinomial tree. Trinomial lattice is defined by {@code LatticeSpecification} 
 * and the option to price is specified by {@code OptionFunction}. 
 * <p>
 * Option pricing with non-uniform tree is realised by specifying {@code RecombiningTrinomialTreeData}.
 */
public class TrinomialTree {
  
  private static final double BUMP = 1.0e-6;
  /**
   * Price an option under the specified trinomial lattice.
   * <p>
   * It is assumed that the volatility, interest rate and continuous dividend rate are constant 
   * over the lifetime of the option.
   * 
   * @param function  the option
   * @param lattice  the lattice specification
   * @param spot  the spot
   * @param volatility  the volatility
   * @param interestRate  the interest rate
   * @param dividendRate  the dividend rate
   * @return the option price
   */
  public double optionPrice(
      OptionFunction function,
      LatticeSpecification lattice,
      double spot,
      double volatility,
      double interestRate,
      double dividendRate) {

    int nSteps = function.getNumberOfSteps();
    double timeToExpiry = function.getTimeToExpiry();
    double dt = timeToExpiry / (double) nSteps;
    double discount = Math.exp(-interestRate * dt);
    DoubleArray params = lattice.getParametersTrinomial(volatility, interestRate - dividendRate, dt);
    double middleFactor = params.get(1);
    double downFactor = params.get(2);
    double upProbability = params.get(3);
    double midProbability = params.get(4);
    double downProbability = params.get(5);
    ArgChecker.isTrue(upProbability > 0d, "upProbability should be greater than 0");
    ArgChecker.isTrue(upProbability < 1d, "upProbability should be smaller than 1");
    ArgChecker.isTrue(midProbability > 0d, "midProbability should be greater than 0");
    ArgChecker.isTrue(midProbability < 1d, "midProbability should be smaller than 1");
    ArgChecker.isTrue(downProbability > 0d, "downProbability should be greater than 0");
    DoubleArray values = function.getPayoffAtExpiryTrinomial(spot, downFactor, middleFactor);
    for (int i = nSteps - 1; i > -1; --i) {
      values = function.getNextOptionValues(discount, upProbability, midProbability, downProbability, values, spot,
          downFactor, middleFactor, i);
    }
    return values.get(0);
  }
  
  public double optionPrice(
      OptionFunction function,
      LatticeSpecification lattice,
      double spot,
      double volatility,
      double interestRate,
      double[] dividendYields,
      double[] dividendTimes) {
    
    int nSteps = function.getNumberOfSteps();
    double timeToExpiry = function.getTimeToExpiry();
    double dt = timeToExpiry / (double) nSteps;
    double discount = Math.exp(-interestRate * dt);
    DoubleArray params = lattice.getParametersTrinomial(volatility, interestRate, dt);
    double middleFactor = params.get(1);
    double downFactor = params.get(2);
    double upProbability = params.get(3);
    double midProbability = params.get(4);
    double downProbability = params.get(5);
    double modifiedSpot = spot;
    for(int i = 0; i < dividendTimes.length; ++i){
      modifiedSpot *= 1 - dividendYields[i];
    }
    ArgChecker.isTrue(upProbability > 0d, "upProbability should be greater than 0");
    ArgChecker.isTrue(upProbability < 1d, "upProbability should be smaller than 1");
    ArgChecker.isTrue(midProbability > 0d, "midProbability should be greater than 0");
    ArgChecker.isTrue(midProbability < 1d, "midProbability should be smaller than 1");
    ArgChecker.isTrue(downProbability > 0d, "downProbability should be greater than 0");
    DoubleArray values = function.getPayoffAtExpiryTrinomial(modifiedSpot, downFactor, middleFactor);
    for (int i = nSteps - 1; i > -1; --i) {
      double dividendMultiplier = 1;
      for(int j = 0; j < dividendTimes.length; ++j){
        if(i * dt > dividendTimes[j]){
          dividendMultiplier *= (1 - dividendYields[j]);
        }
      }
        
      values = function.getNextOptionValues(discount, upProbability, midProbability, downProbability, values, spot * dividendMultiplier,
          downFactor, middleFactor, i);
    }
    return values.get(0);
  }
  
  public ValueDerivatives optionPriceAdjoint(
      OptionFunction function,
      LatticeSpecification lattice,
      double spot,
      double volatility,
      double interestRate,    
      double[] dividendYields,
      double[] dividendTimes) {
    
    int nSteps = function.getNumberOfSteps();
    double timeToExpiry = function.getTimeToExpiry();
    double dt = timeToExpiry / (double) nSteps;
    double discount = Math.exp(-interestRate * dt);
    DoubleArray params = lattice.getParametersTrinomial(volatility, interestRate, dt);
    double upFactor = params.get(0);
    double middleFactor = params.get(1);
    double downFactor = params.get(2);
    double upProbability = params.get(3);
    double midProbability = params.get(4);
    double downProbability = params.get(5);  
    double modifiedSpot = spot;
    for(int i = 0; i < dividendTimes.length; ++i){
      modifiedSpot *= 1 - dividendYields[i];
    }
    ArgChecker.isTrue(upProbability > 0d, "upProbability should be greater than 0");
    ArgChecker.isTrue(upProbability < 1d, "upProbability should be smaller than 1");
    ArgChecker.isTrue(midProbability > 0d, "midProbability should be greater than 0");
    ArgChecker.isTrue(midProbability < 1d, "midProbability should be smaller than 1");
    ArgChecker.isTrue(downProbability > 0d, "downProbability should be greater than 0");
    DoubleArray values = function.getPayoffAtExpiryTrinomial(modifiedSpot, downFactor, middleFactor);
    double delta = 0d;
    double gamma = 0d;
    double theta = 0d;
    for (int i = nSteps - 1; i > -1; --i) {
      double dividendMultiplier = 1;
      for(int j = 0; j < dividendTimes.length; ++j){
        if(i * dt > dividendTimes[j]){
          dividendMultiplier *= (1 - dividendYields[j]);
        }
      }
      values = function.getNextOptionValues(discount, upProbability, midProbability, downProbability, values, spot * dividendMultiplier,
          downFactor, middleFactor, i);
      //We're assuming here that no dividend drop happens at first step. Crude but fine for now.
      if (i == 1) {
        double d1 = (values.get(2) - values.get(1)) / (spot * upFactor - spot);
        double d2 = (values.get(1) - values.get(0)) / (spot - spot * downFactor);
        delta = 0.5 * (d1 + d2);
        //Get theta and gamma
        gamma = (d1 - d2)/(0.5 * spot * (upFactor - downFactor));
        theta = values.get(1);
      }
    }
    theta = (theta - values.get(0)) / dt / 365;
    
    //Perform bump analysis for 
    double bumpVol = optionPrice(function, lattice, spot, volatility + BUMP, interestRate, dividendYields, dividendTimes);
    double bumpRate = optionPrice(function, lattice, spot, volatility, interestRate + BUMP, dividendYields, dividendTimes);
    double vega = (bumpVol - values.get(0)) / BUMP;
    double rho = (bumpRate - values.get(0)) / BUMP;
    return ValueDerivatives.of(values.get(0), DoubleArray.of(delta, vega, rho, theta, gamma));
  }
  
  public ValueDerivatives optionPriceAdjoint(
      OptionFunction function,
      LatticeSpecification lattice,
      double spot,
      double volatility,
      double interestRate,
      double dividendRate) {
    
    int nSteps = function.getNumberOfSteps();
    double timeToExpiry = function.getTimeToExpiry();
    double dt = timeToExpiry / (double) nSteps;
    double discount = Math.exp(-interestRate * dt);
    DoubleArray params = lattice.getParametersTrinomial(volatility, interestRate - dividendRate, dt);
    double upFactor = params.get(0);
    double middleFactor = params.get(1);
    double downFactor = params.get(2);
    double upProbability = params.get(3);
    double midProbability = params.get(4);
    double downProbability = params.get(5);
    ArgChecker.isTrue(upProbability > 0d, "upProbability should be greater than 0");
    ArgChecker.isTrue(upProbability < 1d, "upProbability should be smaller than 1");
    ArgChecker.isTrue(midProbability > 0d, "midProbability should be greater than 0");
    ArgChecker.isTrue(midProbability < 1d, "midProbability should be smaller than 1");
    ArgChecker.isTrue(downProbability > 0d, "downProbability should be greater than 0");
    DoubleArray values = function.getPayoffAtExpiryTrinomial(spot, downFactor, middleFactor);
    double delta = 0d;
    double gamma = 0d;
    double theta = 0d;
    for (int i = nSteps - 1; i > -1; --i) {
      values = function.getNextOptionValues(discount, upProbability, midProbability, downProbability, values, spot,
          downFactor, middleFactor, i);
      if (i == 1) {
        double d1 = (values.get(2) - values.get(1)) / (spot * upFactor - spot);
        double d2 = (values.get(1) - values.get(0)) / (spot - spot * downFactor);
        delta = 0.5 * (d1 + d2);  
        //Get theta and gamma
        gamma = (d1 - d2)/(0.5 * spot * (upFactor - downFactor));
        theta = values.get(1);
      }
    }
    theta = (theta - values.get(0)) / dt / 365;
    
    //Perform bump analysis for 
    double bumpVol = optionPrice(function, lattice, spot, volatility + BUMP, interestRate, dividendRate);
    double bumpRate = optionPrice(function, lattice, spot, volatility, interestRate + BUMP, dividendRate);
    double vega = (bumpVol - values.get(0)) / BUMP;
    double rho = (bumpRate - values.get(0)) / BUMP;
    return ValueDerivatives.of(values.get(0), DoubleArray.of(delta, vega, rho, theta, gamma));
  }

  /**
   * Price an option under the specified trinomial tree gird.
   * 
   * @param function  the option
   * @param data  the trinomial tree data
   * @return the option price
   */
  public double optionPrice(
      OptionFunction function,
      RecombiningTrinomialTreeData data) {

    int nSteps = data.getNumberOfSteps();
    ArgChecker.isTrue(nSteps == function.getNumberOfSteps(), "mismatch in number of steps");
    DoubleArray values = function.getPayoffAtExpiryTrinomial(data.getStateValueAtLayer(nSteps));
    for (int i = nSteps - 1; i > -1; --i) {
      values = function.getNextOptionValues(
          data.getDiscountFactorAtLayer(i), data.getProbabilityAtLayer(i), data.getStateValueAtLayer(i), values, i);
    }
    return values.get(0);
  }

  /**
   * Compute option price and delta under the specified trinomial tree gird.
   * <p>
   * The delta is the first derivative of the price with respect to spot, and approximated by the data embedded in 
   * the trinomial tree.
   * 
   * @param function  the option
   * @param data  the trinomial tree data
   * @return the option price and spot delta
   */
  public ValueDerivatives optionPriceAdjoint(
      OptionFunction function,
      RecombiningTrinomialTreeData data) {

    int nSteps = data.getNumberOfSteps();
    ArgChecker.isTrue(nSteps == function.getNumberOfSteps(), "mismatch in number of steps");
    DoubleArray values = function.getPayoffAtExpiryTrinomial(data.getStateValueAtLayer(nSteps));
    double delta = 0d;
    for (int i = nSteps - 1; i > -1; --i) {
      values = function.getNextOptionValues(
          data.getDiscountFactorAtLayer(i), data.getProbabilityAtLayer(i), data.getStateValueAtLayer(i), values, i);
      if (i == 1) {
        DoubleArray stateValue = data.getStateValueAtLayer(1);
        double d1 = (values.get(2) - values.get(1)) / (stateValue.get(2) - stateValue.get(1));
        double d2 = (values.get(1) - values.get(0)) / (stateValue.get(1) - stateValue.get(0));
        delta = 0.5 * (d1 + d2);
      }
    }
    return ValueDerivatives.of(values.get(0), DoubleArray.of(delta));
  }
}
