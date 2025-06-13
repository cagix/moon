(ns cdq.entity)

(defmulti create (fn [[k] ctx]
                   k))
(defmethod create :default [[_ v] ctx]
  v)

(defmulti create! (fn [[k] eid ctx]
                    k))
(defmethod create! :default [_ eid ctx])

(defmulti destroy! (fn [[k] eid ctx]
                    k))
(defmethod destroy! :default [_ eid ctx])

(defprotocol Entity
  (position [_])
  (in-range? [_ target maxrange])
  (overlaps? [_ other-entity])
  (rectangle [_])
  (id [_])
  (faction [_])
  (enemy [_])
  (state-k [_])
  (state-obj [_])
  (skill-usable-state [_ skill effect-ctx])
  (mod-add    [_ mods])
  (mod-remove [_ mods])
  (stat [_ k])
  (mana [_])
  (mana-val [_])
  (hitpoints [_])
  (pay-mana-cost [_ cost]))

(defn set-movement [entity movement-vector]
  (assoc entity :entity/movement {:direction movement-vector
                                  :speed (or (stat entity :entity/movement-speed) 0)}))

(defn add-skill [entity {:keys [property/id] :as skill}]
  {:pre [(not (contains? (:entity/skills entity) id))]}
  (assoc-in entity [:entity/skills id] skill))
