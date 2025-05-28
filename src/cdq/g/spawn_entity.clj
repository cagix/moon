(ns cdq.g.spawn-entity
  (:require [cdq.ctx :as ctx]
            [cdq.effect :as effect]
            [cdq.entity :as entity]
            [cdq.g :as g]
            [cdq.malli :as m]
            [cdq.modifiers :as modifiers]
            [cdq.vector2 :as v]
            gdl.application
            [gdl.math :as math]
            [gdl.utils :as utils]))

; TODO what about components which get added later/??
; => validate?
; => :entity/id ... body
(def ^:private components-schema
  (m/schema [:map {:closed true}
             [:entity/image {:optional true} :some]
             [:entity/animation {:optional true} :some]
             [:entity/delete-after-animation-stopped? {:optional true} :some]
             [:entity/alert-friendlies-after-duration {:optional true} :some]
             [:entity/line-render {:optional true} :some]
             [:entity/delete-after-duration {:optional true} :some]
             [:entity/destroy-audiovisual {:optional true} :some]
             [:entity/fsm {:optional true} :some]
             [:entity/player? {:optional true} :some]
             [:entity/free-skill-points {:optional true} :some]
             [:entity/click-distance-tiles {:optional true} :some]
             [:entity/clickable {:optional true} :some]
             [:property/id {:optional true} :some]
             [:property/pretty-name {:optional true} :some]
             [:creature/level {:optional true} :some]
             [:entity/faction {:optional true} :some]
             [:entity/species {:optional true} :some]
             [:entity/movement {:optional true} :some]
             [:entity/skills {:optional true} :some]
             [:entity/color {:optional true} :some]

             ; Should each stat have its own modifiers
             ; or :entity/stats with modifiers in one place & all stats?
             ; => first isolate stats/modifiers from the rest of the code
             ; => also how it is used (whats the 'API' for my stats -> info-text also etc?)


             ; vimgrep/entity\/\(attack-speed\|cast-speed\|aggro-range\|movement-speed\|strength\|reaction-time\|hp\|mana\|armor-save\|armor-pierce\|modifiers\)/g src/** test/**

             [:entity/hp {:optional true} :some]
             ; -> is not only used as stat but also @ damage altered/checked
             ; => damage == function inside creature stats, not effect ....

             [:entity/mana {:optional true} :some]
             ; current value read/ changed

             [:entity/movement-speed {:optional true} :some]
             [:entity/aggro-range {:optional true} :some]
             [:entity/reaction-time {:optional true} :some]
             [:entity/strength     {:optional true} :some]
             [:entity/cast-speed   {:optional true} :some]
             [:entity/attack-speed {:optional true} :some]
             [:entity/armor-save   {:optional true} :some]
             [:entity/armor-pierce {:optional true} :some]

             [:entity/modifiers    {:optional true} :some]

             [:entity/inventory    {:optional true} :some]
             [:entity/item {:optional true} :some]
             [:entity/projectile-collision {:optional true} :some]]))

(defn- not-enough-mana? [entity {:keys [skill/cost]}]
  (and cost (> cost (entity/mana-val entity))))

(defrecord Body [position
                 left-bottom

                 width
                 height
                 half-width
                 half-height
                 radius

                 collides?
                 z-order
                 rotation-angle]
  entity/Entity
  (position [_]
    position)

  (rectangle [_]
    (let [[x y] left-bottom]
      (math/rectangle x y width height)))

  (overlaps? [this other-entity]
    (math/overlaps? (entity/rectangle this)
                    (entity/rectangle other-entity)))

  (in-range? [entity target* maxrange] ; == circle-collides?
    (< (- (float (v/distance (entity/position entity)
                             (entity/position target*)))
          (float (:radius entity))
          (float (:radius target*)))
       (float maxrange)))

  (id [{:keys [entity/id]}]
    id)

  (faction [{:keys [entity/faction]}]
    faction)

  (enemy [this]
    (case (entity/faction this)
      :evil :good
      :good :evil))

  (state-k [{:keys [entity/fsm]}]
    (:state fsm))

  (state-obj [this]
    (let [k (entity/state-k this)]
      [k (k this)]))

  (skill-usable-state [entity
                       {:keys [skill/cooling-down? skill/effects] :as skill}
                       effect-ctx]
    (cond
     cooling-down?
     :cooldown

     (not-enough-mana? entity skill)
     :not-enough-mana

     (not (effect/some-applicable? effect-ctx effects))
     :invalid-params

     :else
     :usable))

  (mod-add    [entity mods] (update entity :entity/modifiers modifiers/add    mods))
  (mod-remove [entity mods] (update entity :entity/modifiers modifiers/remove mods))

  (stat [this k]
    (when-let [base-value (k this)]
      (modifiers/get-value base-value
                           (:entity/modifiers this)
                           (keyword "modifier" (name k)))))

  (mana [entity]
    (modifiers/get-mana entity))

  (mana-val [entity]
    (if (:entity/mana entity)
      ((entity/mana entity) 0)
      0))

  (hitpoints [entity]
    (modifiers/get-hitpoints entity))

  (pay-mana-cost [entity cost]
    (let [mana-val ((entity/mana entity) 0)]
      (assert (<= cost mana-val))
      (assoc-in entity [:entity/mana 0] (- mana-val cost)))))

(defn- create-body [{[x y] :position
                     :keys [position
                            width
                            height
                            collides?
                            z-order
                            rotation-angle]}
                    minimum-size
                    z-orders]
  (assert position)
  (assert width)
  (assert height)
  (assert (>= width  (if collides? minimum-size 0)))
  (assert (>= height (if collides? minimum-size 0)))
  (assert (or (boolean? collides?) (nil? collides?)))
  (assert ((set z-orders) z-order))
  (assert (or (nil? rotation-angle)
              (<= 0 rotation-angle 360)))
  (map->Body
   {:position (mapv float position)
    :left-bottom [(float (- x (/ width  2)))
                  (float (- y (/ height 2)))]
    :width  (float width)
    :height (float height)
    :half-width  (float (/ width  2))
    :half-height (float (/ height 2))
    :radius (float (max (/ width  2)
                        (/ height 2)))
    :collides? collides?
    :z-order z-order
    :rotation-angle (or rotation-angle 0)}))

(defn- create-vs [components ctx]
  (reduce (fn [m [k v]]
            (assoc m k (entity/create [k v] ctx)))
          {}
          components))

(extend-type gdl.application.Context
  g/SpawnEntity
  (spawn-entity! [{:keys [ctx/id-counter] :as ctx}
                  position
                  body
                  components]
    (m/validate-humanize components-schema components)
    (assert (and (not (contains? components :position))
                 (not (contains? components :entity/id))))
    (let [eid (atom (-> body
                        (assoc :position position)
                        (create-body ctx/minimum-size ctx/z-orders)
                        (utils/safe-merge (-> components
                                              (assoc :entity/id (swap! id-counter inc))
                                              (create-vs ctx)))))]
      (g/context-entity-add! ctx eid)
      (doseq [component @eid]
        (g/handle-txs! ctx (entity/create! component eid ctx)))
      eid)))
