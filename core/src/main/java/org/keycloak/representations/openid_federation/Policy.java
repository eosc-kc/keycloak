package org.keycloak.representations.openid_federation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.exceptions.MetadataPolicyCombinationException;
import org.keycloak.exceptions.MetadataPolicyException;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Policy <T> extends AbstractPolicy<T> {

    private static final String COMMA = ",";

    private T value;

    @JsonProperty("default")
    private T defaultValue;

    @JsonProperty("one_of")
    protected Set<T> oneOf;

    public static <T> PolicyBuilder<T> builder() {
        return new PolicyBuilder<T>();
    }

    public Policy() {

    }

    protected Policy(PolicyBuilder<T> builder) {
        this.oneOf = builder.one_of.isEmpty() ? null : builder.one_of;
        this.value = builder.value;
        this.defaultValue = builder.defaultValue;
        this.essential = builder.essential;
    }

    public Policy<T> combinePolicy(Policy<T> inferior) throws MetadataPolicyCombinationException {

        if (inferior == null) return this;
        // first check combination value with one_of, subset_of , superset_of
        if (notNullnotEqual(value, inferior.getValue())) {
            throw new MetadataPolicyCombinationException("Could not combine two different values");
        }

        if (notNullnotEqual(defaultValue, inferior.getDefaultValue())) {
            throw new MetadataPolicyCombinationException("Could not construct two different values");
        } else if (defaultValue == null) {
            defaultValue = inferior.getDefaultValue();
        }

        // combine one_of
        if (inferior.getOneOf() != null && oneOf != null) {
            oneOf = oneOf.stream().filter(inferior.getOneOf()::contains).collect(Collectors.toSet());
            if (oneOf.isEmpty()) {
                throw new MetadataPolicyCombinationException("Combination of one_of can not result an empty list");
            }
        } else if (inferior.getOneOf() != null) {
            oneOf = inferior.getOneOf();
        }

        combinePolicyCommon(inferior);

        if (value != null && oneOf != null  && !oneOf.isEmpty() && !oneOf.contains(value)) {
                throw new MetadataPolicyCombinationException("Not null value must be containing in a one_of with values");
        }

        if (illegalValueCombination())
            throw new MetadataPolicyCombinationException("Not null default value must be containing in one_of,subset_of and superset_of, if one of these exist ");

        return policyTypeCombination();
    }

    protected boolean isNotAcceptedCombination(Object defaultValue, Object value) {
        return defaultValue != null && value != null;
    }

    public Policy<T> policyTypeCombination() throws MetadataPolicyCombinationException {
        // same rules as for combination
        if (illegalValueCombination())
            throw new MetadataPolicyCombinationException("Not null default value must be subset of one_of,subset_of and superset of superset_of, if one of these exist ");

        if (isNotAcceptedCombination(defaultValue, value))
            throw new MetadataPolicyCombinationException("False Policy Type Combination exists");

        if (value != null) {
            oneOf = null;
        }

        return this;
    }

    private boolean illegalValueCombination() {
        return value != null && oneOf != null && !oneOf.contains(defaultValue);
    }

    private boolean notNullnotEqual(T superiorValue, T inferiorValue) {
        return superiorValue != null && inferiorValue != null && !superiorValue.equals(inferiorValue);
    }

    public T enforcePolicy(T t, String name) throws MetadataPolicyException {

        if (value != null) {
            return value;
        }

        if (defaultValue != null && t == null) {
            return defaultValue;
        }

        if (oneOf != null && ((t != null && !oneOf.contains(t)) || t == null))
            throw new MetadataPolicyException(name + " must have one values of " + String.join(COMMA, oneOf.toString()));

        if (t == null && essential != null && essential)
            throw new MetadataPolicyException(name + " must exist in rp");

        return t;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(T defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Set<T> getOneOf() {
        return oneOf;
    }

    public void setOneOf(Set<T> oneOf) {
        this.oneOf = oneOf;
    }

    public static class PolicyBuilder<T> {
        private Set<T> one_of = new HashSet<>();
        private Set<T> add = new HashSet<>();
        private T value;
        private T defaultValue;
        private Boolean essential;

        public PolicyBuilder<T> addOneOf(T oneOf) {
            this.one_of.add(oneOf);
            return this;
        }

        public PolicyBuilder<T> addAdd(T add) {
            this.add.add(add);
            return this;
        }

        public PolicyBuilder<T> value(T value) {
            this.value = value;
            return this;
        }

        public PolicyBuilder<T> defaultValue(T defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public PolicyBuilder<T> essential(Boolean essential) {
            this.essential = essential;
            return this;
        }

        public Policy<T> build() {
            Policy<T> policy = new Policy<T>(this);
            return policy;
        }
    }
}

