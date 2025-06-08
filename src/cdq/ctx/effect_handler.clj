(ns cdq.ctx.effect-handler
  (:require [cdq.ctx]
            [cdq.state :as state]
            [cdq.ui.action-bar :as action-bar]
            [cdq.ui.windows.inventory :as inventory-window]
            [cdq.utils :as utils]))

(defmulti do! (fn [[k & _params] _ctx]
                k))

(defn- add-skill! [ctx skill]
  (-> ctx :ctx/stage :action-bar (action-bar/add-skill! skill))
  nil)

(defn- remove-skill! [ctx skill]
  (-> ctx :ctx/stage :action-bar (action-bar/remove-skill! skill))
  nil)

(defn- set-item! [ctx [inventory-cell item]]
  (-> ctx
      :ctx/stage
      :windows
      :inventory-window
      (inventory-window/set-item! inventory-cell {:texture-region (:sprite/texture-region (:entity/image item))
                                                  :tooltip-text (cdq.ctx/info-text ctx item)}))
  nil)

(defn- remove-item! [ctx inventory-cell]
  (-> ctx :ctx/stage :windows :inventory-window (inventory-window/remove-item! inventory-cell))
  nil)

(defn player-state-changed [_ctx new-state-obj]
  (when-let [cursor (state/cursor new-state-obj)]
    [[:tx/set-cursor cursor]]))

(def ctx-handlers {:world.event/player-skill-added add-skill!
                   :world.event/player-skill-removed remove-skill!
                   :world.event/player-item-set set-item!
                   :world.event/player-item-removed remove-item!
                   :world.event/player-state-changed player-state-changed})

(defn handle-txs! [ctx transactions]
  (doseq [transaction transactions
          :when transaction]
    (assert (vector? transaction) (pr-str transaction))
    (try (let [result (do! transaction ctx)]
           (when result
             (let [[world-event-k params] result]
               ;(println world-event-k)
               (handle-txs! ctx
                            ((utils/safe-get ctx-handlers world-event-k) ctx params)))))
         (catch Throwable t
           (throw (ex-info "" {:transaction transaction} t))))))
