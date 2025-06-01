(ns cdq.ui.dev-menu
  (:require [cdq.assets :as assets]
            [cdq.entity :as entity]
            [cdq.g :as g]
            [cdq.application :as application]
            [clojure.string :as str]
            [gdl.utils :as utils]))

(defn create [{:keys [ctx/config] :as ctx}]
  {:menus [{:label "World"
            :items (for [world-fn (:world-fns config)]
                     {:label (str "Start " world-fn)
                      :on-click (fn [_actor _ctx]
                                  (swap! application/state g/reset-game-state! world-fn))})}
           {:label "Help"
            :items [{:label (:info config)}]}
           {:label "Objects"
            :items (for [property-type (sort (g/property-types ctx))]
                     {:label (str/capitalize (name property-type))
                      :on-click (fn [_actor ctx]
                                  (g/open-editor-window! ctx property-type))})}]
   :update-labels [{:label "Mouseover-entity id"
                    :update-fn (fn [{:keys [ctx/mouseover-eid]}]
                                 (when-let [entity (and mouseover-eid @mouseover-eid)]
                                   (entity/id entity)))
                    :icon (assets/texture ctx "images/mouseover.png")}
                   {:label "elapsed-time"
                    :update-fn (fn [ctx]
                                 (str (utils/readable-number (:ctx/elapsed-time ctx)) " seconds"))
                    :icon (assets/texture ctx "images/clock.png")}
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
                    :update-fn g/camera-zoom
                    :icon (assets/texture ctx "images/zoom.png")}
                   {:label "FPS"
                    :update-fn g/frames-per-second
                    :icon (assets/texture ctx "images/fps.png")}]})
