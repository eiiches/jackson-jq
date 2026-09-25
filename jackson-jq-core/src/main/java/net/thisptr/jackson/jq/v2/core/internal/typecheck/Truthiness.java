package net.thisptr.jackson.jq.v2.core.internal.typecheck;

/**
 * What a type says about the branch a condition of that type takes.
 * <p>
 * jq counts only {@code null} and {@code false} as falsy, so most types decide the branch outright.
 * A type deciding it is what lets {@code TypeCheck} route one alternative of a union to one branch.
 */
enum Truthiness {
	/**
	 * Every value the type admits is truthy.
	 */
	ALWAYS_TRUE,

	/**
	 * Every value the type admits is falsy.
	 */
	ALWAYS_FALSE,

	/**
	 * The type admits both, or says too little to tell.
	 */
	UNKNOWN;
}
