(ns anvil.skills
  (:refer-clojure :exclude [contains? remove]))

(defn contains? [{:keys [entity/skills]} {:keys [property/id]}]
  (clojure.core/contains? skills id))

(declare player-add-skill
         player-remove-skill)

(defn add [entity {:keys [property/id] :as skill}]
  {:pre [(not (contains? entity skill))]}
  (when (:entity/player? entity)
    (player-add-skill skill))
  (assoc-in entity [:entity/skills id] skill))

(defn remove [entity {:keys [property/id] :as skill}]
  {:pre [(contains? entity skill)]}
  (when (:entity/player? entity)
    (player-remove-skill skill))
  (update entity :entity/skills dissoc id))
