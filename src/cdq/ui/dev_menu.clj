(ns cdq.ui.dev-menu
  (:require [cdq.ctx :as ctx]
            [cdq.ui.editor :as editor]
            [cdq.utils :as utils]
            [clojure.string :as str]
            [gdl.graphics :as graphics]
            [gdl.graphics.camera :as camera]
            [gdl.graphics.viewport :as viewport]
            [gdl.ui.menu :as menu]))

(defn create []
  (menu/create
   {:menus [{:label "World"
             :items (for [world-fn (:world-fns ctx/config)]
                      {:label (str "Start " (namespace world-fn))
                       :on-click (fn []
                                   ((requiring-resolve 'cdq.application.create.game-state/reset-game!) world-fn))})}
            {:label "Help"
             :items [{:label (:info ctx/config)}]}
            {:label "Objects"
             :items (for [property-type (sort (filter #(= "properties" (namespace %)) (keys (:schemas ctx/db))))]
                      {:label (str/capitalize (name property-type))
                       :on-click (fn []
                                   (editor/open-editor-window! property-type))})}]
    :update-labels [{:label "Mouseover-entity id"
                     :update-fn (fn []
                                  (when-let [entity (and ctx/mouseover-eid @ctx/mouseover-eid)]
                                    (:entity/id entity)))
                     :icon (ctx/assets "images/mouseover.png")}
                    {:label "elapsed-time"
                     :update-fn (fn [] (str (utils/readable-number ctx/elapsed-time) " seconds"))
                     :icon (ctx/assets "images/clock.png")}
                    {:label "paused?"
                     :update-fn (fn [] ctx/paused?)}
                    {:label "GUI"
                     :update-fn (fn [] (mapv int (viewport/mouse-position ctx/ui-viewport)))}
                    {:label "World"
                     :update-fn (fn [] (mapv int (viewport/mouse-position ctx/world-viewport)))}
                    {:label "Zoom"
                     :update-fn (fn [] (camera/zoom (:camera ctx/world-viewport)))
                     :icon (ctx/assets "images/zoom.png")}
                    {:label "FPS"
                     :update-fn (fn [] (graphics/frames-per-second))
                     :icon (ctx/assets "images/fps.png")}]}))

;"Mouseover-Actor: "
#_(when-let [actor (mouse-on-actor? ctx/stage)]
    (str "TRUE - name:" (.getName actor)
         "id: " (user-object actor)))
