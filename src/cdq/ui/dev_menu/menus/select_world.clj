(ns cdq.ui.dev-menu.menus.select-world)

(defn create [_ctx {:keys [world-fns reset-game-fn]}]
  (for [world-fn world-fns]
    {:label (str "Start " (first world-fn))
     :on-click (fn [_actor ctx]
                 (reset-game-fn ctx world-fn))}))
