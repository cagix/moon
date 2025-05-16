(ns cdq.dev-menu-config
  (:require [cdq.ctx :as ctx]
            [cdq.utils :as utils]
            [clojure.string :as str]
            [gdl.graphics :as graphics]
            [gdl.graphics.camera :as camera]
            [gdl.graphics.viewport :as viewport]))

;"Mouseover-Actor: "
#_(when-let [actor (mouse-on-actor? ctx/stage)]
    (str "TRUE - name:" (.getName actor)
         "id: " (user-object actor)))

(defn create []
  {:menus [{:label "World"
            :items (for [world-fn '[cdq.level.vampire/create
                                    cdq.level.uf-caves/create
                                    cdq.level.modules/create]]
                     {:label (str "Start " (namespace world-fn))
                      :on-click (fn [] ((requiring-resolve 'cdq.game/reset-game!) world-fn))})}
           {:label "Help"
            :items [{:label "[W][A][S][D] - Move\n[I] - Inventory window\n[E] - Entity Info window\n[-]/[=] - Zoom\n[P]/[SPACE] - Unpause"}]}
           {:label "Objects"
            :items (for [property-type (sort (filter #(= "properties" (namespace %)) (keys ctx/schemas)))]
                     {:label (str/capitalize (name property-type))
                      :on-click (fn []
                                  ((requiring-resolve 'cdq.editor/open-editor-window!) property-type))})}]
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
                    :icon (ctx/assets "images/fps.png")}]})
