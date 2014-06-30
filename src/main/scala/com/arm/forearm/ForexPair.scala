/**
 * Copyright (c) John Svazic, 2008-2014 - All Rights Reserved 
 */
package com.arm.forearm

case class ForexPair(val pair: String, val multiplier: Int = 10000) {
    require(! pair.toCharArray.exists( _.isLower ), "Bad pair: " + pair)
    require(multiplier > 0)
    
	val base = pair.substring(0, 3)
	val quote = pair.substring(4)
	
	override def toString = base + "_" + quote
}

