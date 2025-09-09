(ns cdq.skills)

(defn add-skill
  [skills
   {:keys [property/id] :as skill}]
  {:pre [(not (contains? skills id))]}
  (assoc skills id skill))
