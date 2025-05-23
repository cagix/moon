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

(defn create [ctx]
  (menu/create
   {:menus [{:label "World"
             :items (for [world-fn (g/config ctx :world-fns)]
                      {:label (str "Start " (namespace world-fn))
                       :on-click (fn [_actor _ctx]
                                   (application/reset-game-state!))})}
            {:label "Help"
             :items [{:label (g/config ctx :info)}]}
            {:label "Objects"
             :items (for [property-type (sort (g/property-types ctx))]
                      {:label (str/capitalize (name property-type))
                       :on-click (fn [_actor ctx]
                                   (editor/open-editor-window! ctx property-type))})}]
    :update-labels [{:label "Mouseover-entity id"
                     :update-fn (fn [{:keys [ctx/mouseover-eid]}]
                                  (when-let [entity (and mouseover-eid @mouseover-eid)]
                                    (:entity/id entity)))
                     :icon (g/texture ctx "images/mouseover.png")}
                    {:label "elapsed-time"
                     :update-fn (fn [ctx]
                                  (str (utils/readable-number (g/elapsed-time ctx)) " seconds"))
                     :icon (g/texture ctx "images/clock.png")}
                    {:label "paused?"
                     :update-fn (fn [{:keys [ctx/paused?]}]
                                  paused?)}
                    {:label "GUI"
                     :update-fn (fn [ctx] (mapv int (g/ui-mouse-position ctx)))}
                    {:label "World"
                     :update-fn (fn [ctx] (mapv int (g/world-mouse-position ctx)))}
                    {:label "Zoom"
                     :update-fn g/camera-zoom
                     :icon (g/texture ctx "images/zoom.png")}
                    {:label "FPS"
                     :update-fn (fn [_ctx]
                                  (graphics/frames-per-second))
                     :icon (g/texture ctx "images/fps.png")}]}))

;"Mouseover-Actor: "
#_(when-let [actor (mouse-on-actor? ctx/stage)]
    (str "TRUE - name:" (.getName actor)
         "id: " (user-object actor)))
