(ns cdq.ui.dev-menu
  (:require [cdq.application :as application]
            [cdq.entity :as entity]
            [cdq.game]
            [cdq.graphics]
            [cdq.ui.editor]
            [clojure.string :as str]
            [gdl.assets :as assets]
            [gdl.ctx :as ctx]
            [gdl.db :as db]
            [gdl.graphics :as graphics]
            [gdl.utils :as utils]))

(defn create [{:keys [ctx/assets
                      ctx/config
                      ctx/db]}]
  {:menus [{:label "World"
            :items (for [world-fn (:world-fns config)]
                     {:label (str "Start " world-fn)
                      :on-click (fn [_actor _ctx]
                                  (swap! application/state cdq.game/reset-game-state! world-fn))})}
           {:label "Help"
            :items [{:label (:info config)}]}
           {:label "Objects"
            :items (for [property-type (sort (db/property-types db))]
                     {:label (str/capitalize (name property-type))
                      :on-click (fn [_actor ctx]
                                  (cdq.ui.editor/open-editor-window! ctx property-type))})}]
   :update-labels [{:label "Mouseover-entity id"
                    :update-fn (fn [{:keys [ctx/mouseover-eid]}]
                                 (when-let [entity (and mouseover-eid @mouseover-eid)]
                                   (entity/id entity)))
                    :icon (assets/texture assets "images/mouseover.png")}
                   {:label "elapsed-time"
                    :update-fn (fn [ctx]
                                 (str (utils/readable-number (:ctx/elapsed-time ctx)) " seconds"))
                    :icon (assets/texture assets "images/clock.png")}
                   {:label "paused?"
                    :update-fn (fn [{:keys [ctx/paused?]}]
                                 paused?)}
                   {:label "GUI"
                    :update-fn (fn [ctx]
                                 (mapv int (ctx/ui-mouse-position ctx)))}
                   {:label "World"
                    :update-fn (fn [ctx]
                                 (mapv int (ctx/world-mouse-position ctx)))}
                   {:label "Zoom"
                    :update-fn cdq.graphics/camera-zoom
                    :icon (assets/texture assets "images/zoom.png")}
                   {:label "FPS"
                    :update-fn (comp graphics/frames-per-second :ctx/graphics)
                    :icon (assets/texture assets "images/fps.png")}]})
