(ns cdq.ui.dev-menu
  (:require [cdq.application :as application]
            [cdq.editor :as editor]
            [cdq.entity :as entity]
            [cdq.game]
            [cdq.graphics :as graphics]
            [clojure.string :as str]
            [gdl.assets :as assets]
            [gdl.db :as db]
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
                                  (editor/open-editor-window! ctx property-type))})}]
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
                                 (mapv int (graphics/ui-mouse-position ctx)))}
                   {:label "World"
                    :update-fn (fn [ctx]
                                 (mapv int (graphics/world-mouse-position ctx)))}
                   {:label "Zoom"
                    :update-fn graphics/camera-zoom
                    :icon (assets/texture assets "images/zoom.png")}
                   {:label "FPS"
                    :update-fn graphics/frames-per-second
                    :icon (assets/texture assets "images/fps.png")}]})
