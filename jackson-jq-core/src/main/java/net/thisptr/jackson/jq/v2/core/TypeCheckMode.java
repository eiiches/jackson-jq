package net.thisptr.jackson.jq.v2.core;

/**
 * Controls compile-time type inference and validation.
 */
public enum TypeCheckMode {
	/**
	 * Skip type inference and validation.
	 */
	OFF,
	/**
	 * Infer types and report type problems as warnings.
	 */
	WARN,
	/**
	 * Infer types, report type problems as errors, and reject an invalid query.
	 */
	STRICT;
}
