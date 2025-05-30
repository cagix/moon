(ns cdq.ui.dev-menu
  (:require [cdq.db :as db]
            [cdq.entity :as entity]
            [cdq.g :as g]
            [cdq.ui.editor :as editor]
            [clojure.string :as str]
            [gdl.graphics :as graphics]
            [gdl.ui.menu :as menu]
            [gdl.utils :as utils]))

(defn create [{:keys [ctx/config
                      ctx/db] :as ctx}]
  (menu/create
   {:menus [{:label "World"
             :items (for [world-fn (:world-fns config)]
                      {:label (str "Start " world-fn)
                       :on-click (fn [_actor _ctx]
                                   (swap! gdl.application/state g/reset-game-state! world-fn))})}
            {:label "Help"
             :items [{:label (:info config)}]}
            {:label "Objects"
             :items (for [property-type (sort (db/property-types db))]
                      {:label (str/capitalize (name property-type))
                       :on-click (fn [_actor ctx]
                                   (editor/open-editor-window! ctx property-type))})}]
    :update-labels [{:label "Mouseover-entity id"
                     :update-fn (fn [{:keys [ctx/mouseover-eid]}]
                                  (when-let [entity (and mouseover-eid @mouseover-eid)]
                                    (entity/id entity)))
                     :icon (g/texture ctx "images/mouseover.png")}
                    {:label "elapsed-time"
                     :update-fn (fn [ctx]
                                  (str (utils/readable-number (:ctx/elapsed-time ctx)) " seconds"))
                     :icon (g/texture ctx "images/clock.png")}
                    {:label "paused?"
                     :update-fn (fn [{:keys [ctx/paused?]}]
                                  paused?)}
                    {:label "GUI"
                     :update-fn (fn [ctx]
                                  (mapv int (g/ui-mouse-position ctx)))}
                    {:label "World"
                     :update-fn (fn [ctx]
                                  (mapv int (g/world-mouse-position ctx)))}
                    {:label "Zoom"
                     :update-fn (comp graphics/camera-zoom :ctx/graphics)
                     :icon (g/texture ctx "images/zoom.png")}
                    {:label "FPS"
                     :update-fn (comp graphics/frames-per-second :ctx/graphics)
                     :icon (g/texture ctx "images/fps.png")}]}))
