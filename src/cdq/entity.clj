(ns cdq.entity
  (:require [cdq.effect :as effect]
            [cdq.gdx.math.geom :as geom]
            [cdq.gdx.math.vector2 :as v]
            [cdq.modifiers :as modifiers]))

(defn position [{:keys [entity/body]}]
  (:body/position body))

(defn rectangle [{:keys [entity/body]}]
  (geom/body->gdx-rectangle body))

(defn overlaps? [entity other-entity]
  (geom/overlaps? (geom/body->gdx-rectangle (:entity/body entity))
                  (geom/body->gdx-rectangle (:entity/body other-entity))))

(defn in-range? [entity target* maxrange]
  (< (- (float (v/distance (position entity)
                           (position target*)))
        (float (/ (:body/width (:entity/body entity))  2))
        (float (/ (:body/width (:entity/body target*)) 2)))
     (float maxrange)))

(defn id [{:keys [entity/id]}]
  id)

(defn faction [{:keys [entity/faction]}]
  faction)

(defn enemy [entity]
  (case (faction entity)
    :evil :good
    :good :evil))

(defn skill-usable-state [entity
                          {:keys [skill/cooling-down? skill/effects] :as skill}
                          effect-ctx]
  (cond
   cooling-down?
   :cooldown

   (modifiers/not-enough-mana? (:creature/stats entity) skill)
   :not-enough-mana

   (not (effect/some-applicable? effect-ctx effects))
   :invalid-params

   :else
   :usable))

(defn mod-add    [entity mods] (update entity :creature/stats modifiers/add    mods))
(defn mod-remove [entity mods] (update entity :creature/stats modifiers/remove mods))

(defn stat [entity k]
  (modifiers/get-stat-value (:creature/stats entity) k))

(defn mana [entity]
  (modifiers/get-mana (:creature/stats entity)))

(defn mana-val [entity]
  (modifiers/mana-val (:creature/stats entity)))

(defn hitpoints [entity]
  (modifiers/get-hitpoints (:creature/stats entity)))

(defn pay-mana-cost [entity cost]
  (update entity :creature/stats modifiers/pay-mana-cost cost))

(defn set-movement [entity movement-vector]
  (assoc entity :entity/movement {:direction movement-vector
                                  :speed (or (stat entity :entity/movement-speed) 0)}))

(defn add-skill [entity {:keys [property/id] :as skill}]
  {:pre [(not (contains? (:entity/skills entity) id))]}
  (assoc-in entity [:entity/skills id] skill))
