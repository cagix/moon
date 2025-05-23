(ns cdq.ui.dev-menu
  (:require [cdq.application :as application]
            [cdq.g :as g]
            [cdq.ui.editor :as editor]
            [cdq.utils :as utils]
            [clojure.string :as str]
            [gdl.graphics :as graphics]
            [gdl.ui.menu :as menu]))

; The dev menu is actually outside the application
; and we can see the ctx/ state itself
; and is it much more work to restart the whole game instead of one level ?

(defn create [{:keys [ctx/config
                      ctx/db
                      ctx/assets]}]
  (menu/create
   {:menus [{:label "World"
             :items (for [world-fn (:world-fns config)]
                      {:label (str "Start " (namespace world-fn))
                       :on-click (fn [_actor _ctx]
                                   (swap! application/state g/reset-game-state!))})}
            {:label "Help"
             :items [{:label (:info config)}]}
            {:label "Objects"
             :items (for [property-type (sort (filter #(= "properties" (namespace %)) (keys (:schemas db))))]
                      {:label (str/capitalize (name property-type))
                       :on-click (fn [_actor ctx]
                                   (editor/open-editor-window! ctx property-type))})}]
    :update-labels [{:label "Mouseover-entity id"
                     :update-fn (fn [{:keys [ctx/mouseover-eid]}]
                                  (when-let [entity (and mouseover-eid @mouseover-eid)]
                                    (:entity/id entity)))
                     :icon (assets "images/mouseover.png")}
                    {:label "elapsed-time"
                     :update-fn (fn [{:keys [ctx/elapsed-time]}]
                                  (str (utils/readable-number elapsed-time) " seconds"))
                     :icon (assets "images/clock.png")}
                    {:label "paused?"
                     :update-fn (fn [{:keys [ctx/paused?]}]
                                  paused?)}
                    {:label "GUI"
                     :update-fn (fn [ctx] (mapv int (g/ui-mouse-position ctx)))}
                    {:label "World"
                     :update-fn (fn [ctx] (mapv int (g/world-mouse-position ctx)))}
                    {:label "Zoom"
                     :update-fn g/camera-zoom
                     :icon (assets "images/zoom.png")}
                    {:label "FPS"
                     :update-fn (fn [_ctx]
                                  (graphics/frames-per-second))
                     :icon (assets "images/fps.png")}]}))

;"Mouseover-Actor: "
#_(when-let [actor (mouse-on-actor? ctx/stage)]
    (str "TRUE - name:" (.getName actor)
         "id: " (user-object actor)))
