(ns game-state.ecs
  (:require [clj-commons.pretty.repl :as p]
            [utils.core :refer [sort-by-order]]
            [core.component :refer [defcomponent] :as component]
            [api.context :as ctx]
            [api.graphics :as g]
            [api.entity :as entity :refer [map->Entity]]
            [api.tx :refer [transact!]]))

(defn ->state []
  {:context.game/uids-entities {}
   :context.game/entity-error nil})

(defn- uids-entities [ctx] (:context.game/uids-entities ctx))
(defn- entity-error  [ctx] (:context.game/entity-error  ctx))

(defmethod transact! :tx/setup-entity [[_ an-atom uid components] ctx]
  {:pre [(not (contains? components :entity/id))
         (not (contains? components :entity/uid))]}
  (let [entity* (-> components
                    (component/update-map entity/create-component components ctx)
                    map->Entity)]
    (reset! an-atom (assoc entity* :entity/id an-atom :entity/uid uid)))
  ctx)

(defmethod transact! :tx/assoc-uids->entities [[_ entity] ctx]
  {:pre [(number? (:entity/uid @entity))]}
  (update ctx :context.game/uids-entities assoc (:entity/uid @entity) entity))

(defmethod transact! :tx/dissoc-uids->entities [[_ uid] ctx]
  {:pre [(contains? (uids-entities ctx) uid)]}
  (update ctx :context.game/uids-entities dissoc uid))

(defcomponent :entity/uid {}
  (entity/create [_ {:keys [entity/id]} _ctx]
    [[:tx/assoc-uids->entities id]])
  (entity/destroy [[_ uid] _entity* _ctx]
    [[:tx/dissoc-uids->entities uid]]))

(let [cnt (atom 0)]
  (defn- unique-number! []
    (swap! cnt inc)))

(defn- apply-system-transact-all! [ctx system entity*]
  (reduce ctx/transact-all!
          ctx
          (component/apply-system system entity* ctx)))

(defmethod transact! :tx/create [[_ components] ctx]
  (let [entity (atom nil)]
    (-> ctx
        (ctx/transact-all! [[:tx/setup-entity entity (unique-number!) components]])
        (apply-system-transact-all! entity/create @entity))))

(defmethod transact! :tx/destroy [[_ entity] ctx]
  (swap! entity assoc :entity/destroyed? true)
  ctx)

(defn- handle-entity-error! [ctx entity* throwable]
  (p/pretty-pst (ex-info "" (select-keys entity* [:entity/uid]) throwable))
  (assoc ctx :context.game/entity-error throwable))

(defn- render-entity* [system entity* g ctx]
  (try
   (dorun (component/apply-system system entity* g ctx))
   (catch Throwable t
     (when-not (entity-error ctx)
       (handle-entity-error! ctx entity* t)) ; TODO doesnt work assoc tx lost
     (let [[x y] (:entity/position entity*)]
       (g/draw-text g
                    {:text (str "Error / entity uid: " (:entity/uid entity*))
                     :x x
                     :y y
                     :up? true})))))

; TODO similar with define-order --- same fns for z-order keyword ... same name make ?
(def ^:private render-systems [entity/render-below
                               entity/render-default
                               entity/render-above
                               entity/render-info])

(extend-type api.context.Context
  api.context/EntityComponentSystem
  (entity-error [ctx]
    (entity-error ctx))

  (all-entities [ctx]
    (vals (uids-entities ctx)))

  (get-entity [ctx uid]
    (get (uids-entities ctx) uid))

  (tick-entities! [ctx entities*]
    (reduce (fn [ctx entity*]
              (try
               (apply-system-transact-all! ctx entity/tick entity*)
               (catch Throwable t
                 (handle-entity-error! ctx entity* t))))
            ctx
            entities*))

  (render-entities! [context g entities*]
    (doseq [entities* (map second ; FIXME lazy seq
                           (sort-by-order (group-by :entity/z-order entities*)
                                          first
                                          entity/render-order))
            system render-systems
            entity* entities*]
      (render-entity* system entity* g context))
    (doseq [entity* entities*]
      (render-entity* entity/render-debug entity* g context)))

  (remove-destroyed-entities! [ctx]
    (reduce (fn [ctx entity]
              (apply-system-transact-all! ctx entity/destroy @entity))
            ctx
            (filter (comp :entity/destroyed? deref) (ctx/all-entities ctx)))))
