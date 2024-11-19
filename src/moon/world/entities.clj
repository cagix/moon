(ns moon.world.entities
  (:require [clj-commons.pretty.repl :refer [pretty-pst]]
            [moon.db :as db]
            [gdl.math.vector :as v]
            [gdl.utils :refer [sort-by-order safe-merge]]
            [moon.core :refer [draw-rectangle play-sound]]
            [moon.body :as body]
            [moon.entity :as entity]
            [moon.player :as player]
            [moon.projectile :as projectile]
            [moon.world.content-grid :as content-grid]
            [moon.world.grid :as grid]
            [moon.world.line-of-sight :refer [line-of-sight?]]
            [moon.world.time :refer [timer]]))

(declare ids->eids)

(defn all []
  (vals ids->eids))

(defn get-entity [id]
  (get ids->eids id))

(declare content-grid)

(defn active []
  (content-grid/active-entities content-grid @player/eid))

(def ^:private ^:dbg-flag show-body-bounds false)

(defn- draw-body-rect [entity color]
  (let [[x y] (:left-bottom entity)]
    (draw-rectangle x y (:width entity) (:height entity) color)))

(defn- render-entity! [system entity]
  (try
   (when show-body-bounds
     (draw-body-rect entity (if (:collides? entity) :white :gray)))
   (run! #(system % entity) entity)
   (catch Throwable t
     (draw-body-rect entity :red)
     (pretty-pst t 12))))

(defn render
  "Draws entities in the correct z-order and in the order of render-systems for each z-order."
  [entities]
  (let [player @player/eid]
    (doseq [[z-order entities] (sort-by-order (group-by :z-order entities)
                                              first
                                              body/render-z-order)
            system entity/render-systems
            entity entities
            :when (or (= z-order :z-order/effect)
                      (line-of-sight? player entity))]
      (render-entity! system entity))))

; precaution in case a component gets removed by another component
; the question is do we still want to update nil components ?
; should be contains? check ?
; but then the 'order' is important? in such case dependent components
; should be moved together?
(defn- tick-entity [eid]
  (try
   (doseq [k (keys @eid)]
     (try (when-let [v (k @eid)]
            (entity/tick [k v] eid))
          (catch Throwable t
            (throw (ex-info "entity/tick" {:k k} t)))))
   (catch Throwable t
     (throw (ex-info "" (select-keys @eid [:entity/id]) t)))))

(defn tick [entities]
  (run! tick-entity entities))

(defn- add-to-world [eid]
  (let [id (:entity/id @eid)]
    (assert (number? id))
    (alter-var-root #'ids->eids assoc id eid))
  (content-grid/update-entity! content-grid eid)
  ; https://github.com/damn/core/issues/58
  ;(assert (valid-position? grid @eid)) ; TODO deactivate because projectile no left-bottom remove that field or update properly for all
  (grid/add-entity eid))

(defn- remove-from-world [eid]
  (let [id (:entity/id @eid)]
    (assert (contains? ids->eids id))
    (alter-var-root #'ids->eids dissoc id))
  (content-grid/remove-entity! eid)
  (grid/remove-entity eid))

(defn position-changed [eid]
  (content-grid/update-entity! content-grid eid)
  (grid/entity-position-changed eid))

(defn remove-destroyed []
  (doseq [eid (filter (comp :entity/destroyed? deref) (all))]
    (remove-from-world eid)
    (doseq [component @eid]
      (entity/destroy component eid))))

(let [cnt (atom 0)]
  (defn- unique-number! []
    (swap! cnt inc)))

(defn- create-vs [components]
  (reduce (fn [m [k v]]
            (assoc m k (entity/->v [k v])))
          {}
          components))

(defn- create [position body components]
  (assert (and (not (contains? components :position))
               (not (contains? components :entity/id))))
  (let [eid (atom (-> body
                      (assoc :position position)
                      body/create
                      (safe-merge (-> components
                                      (assoc :entity/id (unique-number!))
                                      (create-vs)))))]
    (add-to-world eid)
    (doseq [component @eid]
      (entity/create component eid))
    eid))

(def ^{:doc "For effects just to have a mouseover body size for debugging purposes."
       :private true}
  effect-body-props
  {:width 0.5
   :height 0.5
   :z-order :z-order/effect})

(defn audiovisual [position {:keys [tx/sound entity/animation]}]
  (play-sound sound)
  (create position
          effect-body-props
          {:entity/animation animation
           :entity/delete-after-animation-stopped true}))

; # :z-order/flying has no effect for now
; * entities with :z-order/flying are not flying over water,etc. (movement/air)
; because using potential-field for z-order/ground
; -> would have to add one more potential-field for each faction for z-order/flying
; * they would also (maybe) need a separate occupied-cells if they don't collide with other
; * they could also go over ground units and not collide with them
; ( a test showed then flying OVER player entity )
; -> so no flying units for now
(defn- ->body [{:keys [body/width body/height #_body/flying?]}]
  {:width  width
   :height height
   :collides? true
   :z-order :z-order/ground #_(if flying? :z-order/flying :z-order/ground)})

(defn creature [{:keys [position creature-id components]}]
  (let [props (db/get creature-id)]
    (create position
            (->body (:entity/body props))
            (-> props
                (dissoc :entity/body)
                (assoc :entity/destroy-audiovisual :audiovisuals/creature-die)
                (safe-merge components)))))

(defn item [position item]
  (create position
          {:width 0.75
           :height 0.75
           :z-order :z-order/on-ground}
          {:entity/image (:entity/image item)
           :entity/item item
           :entity/clickable {:type :clickable/item
                              :text (:property/pretty-name item)}}))

(defn shout [position faction duration]
  (create position
          effect-body-props
          {:entity/alert-friendlies-after-duration
           {:counter (timer duration)
            :faction faction}}))

(defn line-render [{:keys [start end duration color thick?]}]
  (create start
          effect-body-props
          #:entity {:line-render {:thick? thick? :end end :color color}
                    :delete-after-duration duration}))

(defn projectile [{:keys [position direction faction]}
                  {:keys [entity/image
                          projectile/max-range
                          projectile/speed
                          entity-effects
                          projectile/piercing?] :as projectile}]
  (let [size (projectile/size projectile)]
    (create position
            {:width size
             :height size
             :z-order :z-order/flying
             :rotation-angle (v/angle-from-vector direction)}
            {:entity/movement {:direction direction
                               :speed speed}
             :entity/image image
             :entity/faction faction
             :entity/delete-after-duration (/ max-range speed)
             :entity/destroy-audiovisual :audiovisuals/hit-wall
             :entity/projectile-collision {:entity-effects entity-effects
                                           :piercing? piercing?}})))
