(ns cdq.ui.dev-menu.menus.select-world
  (:require [cdq.ctx :as ctx]
            [cdq.ctx.world :as world]))

(defn create
  [_ctx
   {:keys [world-fns
           create-world]}]
  (for [world-fn world-fns]
    {:label (str "Start " (first world-fn))
     :on-click (fn [_actor {:keys [ctx/application-state
                                   ctx/world]
                            :as ctx}]
                 (ctx/handle-txs! ctx [[:tx/reset-stage]])
                 (world/dispose! world)
                 (swap! application-state create-world world-fn))}))
