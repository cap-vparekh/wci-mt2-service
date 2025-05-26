/*
 * Copyright 2023 SNOMED International - All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains the property of SNOMED International
 * The intellectual and technical concepts contained herein are proprietary to
 * SNOMED International and may be covered by U.S. and Foreign Patents, patents in process,
 * and are protected by trade secret or copyright law.  Dissemination of this information
 * or reproduction of this material is strictly forbidden.
 */

package org.ihtsdo.refsetservice.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlTransient;

import org.ihtsdo.refsetservice.model.Collection;

/**
 * Represents a list of results. Aligns with "collection".
 *
 * @param <T> the type parameter
 */
public class ResultList<T> implements Collection<T> {

    /** The total count. */
    private int total = 0;

    /** The objects. */
    private List<T> items = null;

    /** The limit. */
    private int limit;

    /** The offset. */
    private int offset;

    /** A miscellaneous field for other counts that might be needed. */
    private int miscCountA;

    /** A miscellaneous field for other counts that might be needed. */
    private int miscCountB;

    /**  The searchAfter. */
    private String searchAfter;

    /** The offset. */
    private boolean totalKnown;

    /** The score map. */
    private Map<String, Float> scoreMap = null;

    /** The time taken. */
    private Long timeTaken;

    /** The parameters. */
    private SearchParameters parameters = null;

    /**
     * Instantiates an empty {@link ResultList}.
     */
    public ResultList() {

        // n/a
    }

    /**
     * Instantiates a {@link ResultList} from the specified parameters.
     *
     * @param items the items
     */
    public ResultList(final List<T> items) {

        this.items = items;
        if (items != null) {
            this.total = items.size();
        } else {
            this.total = 0;
        }
    }

    /**
     * Instantiates a {@link ResultList} from the specified parameters.
     *
     * @param other the other
     */
    public ResultList(final ResultList<T> other) {

        populateFrom(other);
    }

    /**
     * Populate from.
     *
     * @param other the other
     */
    public void populateFrom(final ResultList<T> other) {

        items = other.getItems();
        limit = other.getLimit();
        offset = other.getOffset();
        miscCountA = other.getMiscCountA();
        miscCountB = other.getMiscCountB();
        searchAfter = other.getSearchAfter();
        parameters = other.getParameters();
        scoreMap = other.getScoreMap();
        timeTaken = other.getTimeTaken();
        total = other.getTotal();
        totalKnown = other.isTotalKnown();
    }

    /**
     * Size.
     *
     * @return the int
     */
    public int size() {

        return getItems().size();
    }

    /**
     * Contains.
     *
     * @param element the element
     * @return true, if successful
     */
    public boolean contains(final T element) {

        return items.contains(element);
    }

    /**
     * Sets the items.
     *
     * @param items the items
     */
    @Override
    public void setItems(final List<T> items) {

        this.items = items;
    }

    /**
     * Returns the but in an XML transient way.
     *
     * @return the objects transient
     */
    @Override
    public List<T> getItems() {

        if (items == null) {
            items = new ArrayList<>();
        }
        return items;
    }

    /**
     * Returns the score map.
     *
     * @return the score map
     */
    @XmlTransient
    public Map<String, Float> getScoreMap() {

        if (scoreMap == null) {
            scoreMap = new HashMap<>();
        }
        return scoreMap;
    }

    /**
     * Sets the score map.
     *
     * @param scoreMap the score map
     */
    public void setScoreMap(final Map<String, Float> scoreMap) {

        this.scoreMap = scoreMap;
    }

    /* see superclass */
    @Override
    public int getTotal() {

        return total;
    }

    /**
     * Indicates whether or not total known is the case.
     *
     * @return the totalKnown
     */
    public boolean isTotalKnown() {

        return totalKnown;
    }

    /**
     * Sets the total known.
     *
     * @param totalKnown the totalKnown to set
     */
    public void setTotalKnown(final boolean totalKnown) {

        this.totalKnown = totalKnown;
    }

    /* see superclass */
    @Override
    public void setTotal(final int total) {

        this.total = total;
    }

    /* see superclass */
    @Override
    public int getLimit() {

        return limit;
    }

    /* see superclass */
    @Override
    public void setLimit(final int limit) {

        this.limit = limit;
    }

    /**
     * Returns a miscellaneous field for other counts that might be needed.
     *
     * @return the count
     */
    public int getMiscCountA() {

        return miscCountA;
    }

    /**
     * Sets a miscellaneous field for other counts that might be needed.
     *
     * @param miscCountA the count
     */
    public void setMiscCountA(final int miscCountA) {

        this.miscCountA = miscCountA;
    }

    /**
     * Returns a miscellaneous field for other counts that might be needed.
     *
     * @return the count
     */
    public int getMiscCountB() {

        return miscCountB;
    }

    /**
     * Sets a miscellaneous field for other counts that might be needed.
     *
     * @param miscCountB the count
     */
    public void setMiscCountB(final int miscCountB) {

        this.miscCountB = miscCountB;
    }

    /* see superclass */
    @Override
    public int getOffset() {

        return offset;
    }

    /* see superclass */
    @Override
    public void setOffset(final int offset) {

        this.offset = offset;
    }

    /**
     * Returns the search after.
     *
     * @return the search after
     */
    public String getSearchAfter() {

        return searchAfter;
    }

    /**
     * Sets the search after.
     *
     * @param searchAfter the search after
     */
    public void setSearchAfter(final String searchAfter) {

        this.searchAfter = searchAfter;
    }

    /**
     * Time taken.
     *
     * @return the long
     */
    public Long getTimeTaken() {

        return timeTaken;
    }

    /**
     * Sets the time taken.
     *
     * @param timeTaken the time taken
     */
    public void setTimeTaken(final Long timeTaken) {

        this.timeTaken = timeTaken;
    }

    /**
     * Returns the parameters.
     *
     * @return the parameters
     */
    public SearchParameters getParameters() {

        return parameters;
    }

    /**
     * Sets the parameters.
     *
     * @param parameters the parameters
     */
    public void setParameters(final SearchParameters parameters) {

        this.parameters = parameters;
    }

    /* see superclass */
    @Override
    public int hashCode() {

        final int prime = 31;
        int result = 1;
        result = prime * result + ((items == null) ? 0 : items.hashCode());
        result = prime * result + limit;
        result = prime * result + miscCountA;
        result = prime * result + miscCountB;
        result = prime * result + offset;
        result = prime * result + ((parameters == null) ? 0 : parameters.hashCode());
        result = prime * result + ((scoreMap == null) ? 0 : scoreMap.hashCode());
        result = prime * result + ((searchAfter == null) ? 0 : searchAfter.hashCode());
        result = prime * result + ((timeTaken == null) ? 0 : timeTaken.hashCode());
        result = prime * result + total;
        result = prime * result + (totalKnown ? 1231 : 1237);
        return result;
    }

    /* see superclass */
    @SuppressWarnings("rawtypes")
    @Override
    public boolean equals(final Object obj) {

        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ResultList)) {
            return false;
        }
        final ResultList other = (ResultList) obj;
        if (items == null) {
            if (other.items != null) {
                return false;
            }
        } else if (!items.equals(other.items)) {
            return false;
        }
        if (limit != other.limit) {
            return false;
        }
        if (miscCountA != other.miscCountA) {
            return false;
        }
        if (miscCountB != other.miscCountB) {
            return false;
        }
        if (offset != other.offset) {
            return false;
        }
        if (parameters == null) {
            if (other.parameters != null) {
                return false;
            }
        } else if (!parameters.equals(other.parameters)) {
            return false;
        }
        if (scoreMap == null) {
            if (other.scoreMap != null) {
                return false;
            }
        } else if (!scoreMap.equals(other.scoreMap)) {
            return false;
        }
        if (searchAfter == null) {
            if (other.searchAfter != null) {
                return false;
            }
        } else if (!searchAfter.equals(other.searchAfter)) {
            return false;
        }
        if (timeTaken == null) {
            if (other.timeTaken != null) {
                return false;
            }
        } else if (!timeTaken.equals(other.timeTaken)) {
            return false;
        }
        if (total != other.total) {
            return false;
        }
        if (totalKnown != other.totalKnown) {
            return false;
        }
        return true;
    }

    /* see superclass */
    @Override
    public String toString() {

        try {
            return ModelUtility.toJson(this);
        } catch (final Exception e) {
            return e.getMessage();
        }
    }

}
