package org.keycloak.representations.openid_federation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.exceptions.MetadataPolicyCombinationException;
import org.keycloak.exceptions.MetadataPolicyException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class PolicyList<T> extends AbstractPolicy<T> {

    private static final String COMMA = ",";

    private Set<T> value;

    @JsonProperty("default")
    private Set<T> defaultValue;

    protected Set<T> add;

    @JsonProperty("subset_of")
    protected Set<T> subsetOf;

    @JsonProperty("superset_of")
    protected Set<T> supersetOf;

    public PolicyList() {

    }

    public PolicyList<T> combinePolicy(PolicyList<T> inferior) throws MetadataPolicyCombinationException {
        if (inferior == null) {
            return this;
        }

        if (notNullnotEqual(value, inferior.getValue())) {
            throw new MetadataPolicyCombinationException("Could not combine two different values");
        }

        if (notNullnotEqual(defaultValue, inferior.getDefaultValue())) {
            throw new MetadataPolicyCombinationException("Could not construct two different values");
        } else if (defaultValue == null) {
            defaultValue = inferior.getDefaultValue();
        }

        // combine add
        if (add == null) {
            add = inferior.getAdd();
        } else if (inferior.getAdd() != null) {
            add.addAll(inferior.getAdd());
        }

        // combine subset_of
        if (inferior.getSubsetOf() != null && subsetOf != null) {
            subsetOf = subsetOf.stream().filter(inferior.getSubsetOf()::contains).collect(Collectors.toSet());
            if (subsetOf.isEmpty()) {
                subsetOf = null;
            }
        } else if (inferior.getSubsetOf() != null) {
            subsetOf = inferior.getSubsetOf();
        }

        // combine superset_of
        if (inferior.getSupersetOf() != null && supersetOf != null) {
            supersetOf.addAll(inferior.getSupersetOf());
        } else if (inferior.getSupersetOf() != null) {
            supersetOf = inferior.getSupersetOf();
        }

        combinePolicyCommon(inferior);

        if (value == null) {
            if (inferior.getValue() != null && ((subsetOf != null && !subsetOf.containsAll(inferior.getValue())) || (supersetOf != null && !supersetOf.containsAll(inferior.getValue()))))
                throw new MetadataPolicyCombinationException("Inferior value must be subset of one_of,subset_of and superset_of, if one of these exist ");
            value = inferior.getValue();
        }

        return policyTypeCombination();
    }

    public PolicyList<T> policyTypeCombination() throws MetadataPolicyCombinationException {
        if (illegalValueCombination())
            throw new MetadataPolicyCombinationException("Not null value must be subset of one_of,subset_of and superset of superset_of, if one of these exist ");

        if (isNotAcceptedCombination())
            throw new MetadataPolicyCombinationException("False Policy Type Combination exists");

        return this;
    }

    protected boolean isNotAcceptedCombination() {
        return (add != null && !add.isEmpty() && ((value != null && !value.isEmpty() && !value.containsAll(add)) || (subsetOf != null && !subsetOf.isEmpty() && subsetOf.containsAll(add)))
         || (value != null && !value.isEmpty() && supersetOf != null && supersetOf.isEmpty()) && !value.containsAll(supersetOf))
                || (supersetOf != null && supersetOf.isEmpty() && subsetOf != null && !subsetOf.isEmpty() && supersetOf.containsAll(subsetOf));
    }

    private boolean illegalValueCombination() {
        return value != null && ((subsetOf != null && !subsetOf.containsAll(value)) || (supersetOf != null && !defaultValue.containsAll(supersetOf)));
    }

    private boolean notNullnotEqual(Set<T> superiorValue, Set<T> inferiorValue) {
        return superiorValue != null && inferiorValue != null && !(superiorValue.size() == inferiorValue.size() && superiorValue.containsAll(inferiorValue));
    }

    public List<T> enforcePolicy(List<T> t, String name) throws MetadataPolicyException {

        if (value != null) {
            return new ArrayList<>(value);
        }

        //add can only exist alone
        if (add != null) {
            if (t == null) t = new ArrayList<>();
            for (T val : add) {
                if (!t.contains(val)) t.add(val);
            }
            return t;
        }

        if (defaultValue != null && t == null) {
            return new ArrayList<>(defaultValue);
        }

        if (supersetOf != null && (t == null || !t.containsAll(supersetOf)))
            throw new MetadataPolicyException(name + " values must be superset of " + supersetOf.stream().map(String::valueOf).collect(Collectors.joining(COMMA)));

        if (subsetOf != null && t != null) {
            return t.stream().filter(e -> subsetOf.contains(e)).collect(Collectors.toList());
        }

        if ((t == null || t.isEmpty()) && essential != null && essential)
            throw new MetadataPolicyException(name + " must exist in rp");

        return t;
    }

    public Set<T> getValue() {
        return value;
    }

    public void setValue(Set<T> value) {
        this.value = value;
    }

    public Set<T> getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Set<T> defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Set<T> getAdd() {
        return add;
    }

    public void setAdd(Set<T> add) {
        this.add = add;
    }

    public Set<T> getSubsetOf() {
        return subsetOf;
    }

    public void setSubsetOf(Set<T> subsetOf) {
        this.subsetOf = subsetOf;
    }

    public Set<T> getSupersetOf() {
        return supersetOf;
    }

    public void setSupersetOf(Set<T> supersetOf) {
        this.supersetOf = supersetOf;
    }
}
