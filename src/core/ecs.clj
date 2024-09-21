(ns core.ecs
  (:require [clj-commons.pretty.repl :as p]
            [utils.core :refer [safe-merge sort-by-order]]
            [core.component :refer [defcomponent] :as component]
            [core.context :as ctx]
            [core.g :as g]
            [core.entity :as entity]
            [core.tx :as tx])
  (:import com.badlogic.gdx.graphics.Color))

(def ^:private this :context/ecs)

(defcomponent this
  (component/create [_ _ctx]
    {}))

(defn- entities [ctx] (this ctx))

(defcomponent :entity/uid
  {:let uid}
  (entity/create [_ entity ctx]
    (assert (number? uid))
    (update ctx this assoc uid entity))

  (entity/destroy [_ _entity ctx]
    (assert (contains? (entities ctx) uid))
    (update ctx this dissoc uid)))

(let [cnt (atom 0)]
  (defn- unique-number! []
    (swap! cnt inc)))

(defcomponent :entity/id
  (entity/create  [[_ id] _eid _ctx] [[:tx/add-to-world      id]])
  (entity/destroy [[_ id] _eid _ctx] [[:tx/remove-from-world id]]))

(defn- create-e-system [eid]
  (for [component @eid]
    (fn [ctx]
      ; we are assuming components dont remove other ones at entity/create
      ; thats why we reuse component and not fetch each time again for key
      (entity/create component eid ctx))))

(defcomponent :tx/create
  (tx/do! [[_ position body components] ctx]
    (assert (and (not (contains? components :position))
                 (not (contains? components :entity/id))
                 (not (contains? components :entity/uid))))
    (let [eid (atom nil)]
      (reset! eid (-> body
                      (assoc :position position)
                      entity/->Body
                      (safe-merge (-> components
                                      (assoc :entity/id eid
                                             :entity/uid (unique-number!))
                                      (component/create-vs ctx)))))
      (create-e-system eid))))

(defcomponent :tx/destroy
  (tx/do! [[_ entity] ctx]
    [[:tx/assoc entity :entity/destroyed? true]]))

(def ^:private ^:dbg-flag show-body-bounds false)

(defn- draw-body-rect [g entity* color]
  (let [[x y] (:left-bottom entity*)]
    (g/draw-rectangle g x y (:width entity*) (:height entity*) color)))

(defn- render-entity* [system entity* g ctx]
  (try
   (when show-body-bounds
     (draw-body-rect g entity* (if (:collides? entity*) Color/WHITE Color/GRAY)))
   (run! #(system % entity* g ctx) entity*)
   (catch Throwable t
     (draw-body-rect g entity* Color/RED)
     (p/pretty-pst t 12))))

(defn- tick-system [ctx entity]
  (try
   (reduce (fn do-tick-component [ctx k]
             ; precaution in case a component gets removed by another component
             ; the question is do we still want to update nil components ?
             ; should be contains? check ?
             ; but then the 'order' is important? in such case dependent components
             ; should be moved together?
             (if-let [v (k @entity)]
               (let [component [k v]]
                 (ctx/do! ctx (entity/tick component entity ctx)))
               ctx))
           ctx
           (keys @entity))
   (catch Throwable t
     (throw (ex-info "" (select-keys @entity [:entity/uid]) t))
     ctx)))

(extend-type core.context.Context
  core.context/EntityComponentSystem
  (all-entities [ctx]
    (vals (entities ctx)))

  (get-entity [ctx uid]
    (get (entities ctx) uid))

  (tick-entities! [ctx entities]
    (reduce tick-system ctx entities))

  (render-entities! [ctx g entities*]
    (let [player-entity* (ctx/player-entity* ctx)]
      (doseq [[z-order entities*] (sort-by-order (group-by :z-order entities*)
                                                 first
                                                 entity/render-order)
              system entity/render-systems
              entity* entities*
              :when (or (= z-order :z-order/effect)
                        (ctx/line-of-sight? ctx player-entity* entity*))]
        (render-entity* system entity* g ctx))))

  (remove-destroyed-entities! [ctx]
    (for [entity (filter (comp :entity/destroyed? deref) (ctx/all-entities ctx))
          component @entity]
      (fn [ctx]
        (entity/destroy component entity ctx)))))

(defcomponent :tx/assoc
  (tx/do! [[_ entity k v] ctx]
    (assert (keyword? k))
    (swap! entity assoc k v)
    ctx))

(defcomponent :tx/assoc-in
  (tx/do! [[_ entity ks v] ctx]
    (swap! entity assoc-in ks v)
    ctx))

(defcomponent :tx/dissoc
  (tx/do! [[_ entity k] ctx]
    (assert (keyword? k))
    (swap! entity dissoc k)
    ctx))

(defcomponent :tx/dissoc-in
  (tx/do! [[_ entity ks] ctx]
    (assert (> (count ks) 1))
    (swap! entity update-in (drop-last ks) dissoc (last ks))
    ctx))

(defcomponent :tx/update-in
  (tx/do! [[_ entity ks f] ctx]
    (swap! entity update-in ks f)
    ctx))