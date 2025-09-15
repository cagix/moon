(ns cdq.ui.dev-menu.menus.select-world
  (:require [cdq.ctx :as ctx]))

(defn create [_ctx {:keys [world-fns]}]
  (for [world-fn world-fns]
    {:label (str "Start " (first world-fn))
     :on-click (fn [_actor ctx]
                 (ctx/handle-txs! ctx [[:tx/reset-game-state world-fn]]))}))
