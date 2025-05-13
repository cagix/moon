(ns cdq.stage
  (:require [cdq.ctx :as ctx]
            [cdq.entity :as entity]
            [cdq.entity.state :as state]
            [cdq.graphics :as graphics]
            [cdq.ui.action-bar :as action-bar]
            [cdq.ui.entity-info-window :as entity-info-window]
            [cdq.ui.inventory-window :as inventory-window]
            [cdq.ui.player-message :as player-message]
            [cdq.ui.hp-mana-bar :as hp-mana-bar]
            [clojure.gdx :as gdx]
            [clojure.gdx.input :as input]
            [clojure.gdx.scene2d.actor :as actor]
            [clojure.gdx.scene2d.stage :as stage]
            [clojure.gdx.scene2d.ui :as ui]
            [clojure.gdx.scene2d.ui.menu :as ui.menu]
            [clojure.gdx.graphics :as gdx.graphics]
            [clojure.gdx.graphics.camera :as camera]
            [clojure.string :as str]
            [clojure.utils :refer [readable-number]]))

;"Mouseover-Actor: "
#_(when-let [actor (cdq.stage/mouse-on-actor? ctx/stage)]
    (str "TRUE - name:" (.getName actor)
         "id: " (user-object actor)))

(defn- dev-menu-config []
  {:menus [{:label "World"
            :items (for [world-fn '[cdq.level.vampire/create
                                    cdq.level.uf-caves/create
                                    cdq.level.modules/create]]
                     {:label (str "Start " (namespace world-fn))
                      :on-click (fn [] (ctx/reset-game! world-fn))})}
           {:label "Help"
            :items [{:label "[W][A][S][D] - Move\n[I] - Inventory window\n[E] - Entity Info window\n[-]/[=] - Zoom\n[P]/[SPACE] - Unpause"}]}
           {:label "Objects"
            :items (for [property-type (sort (filter #(= "properties" (namespace %))
                                                     (keys (:schemas ctx/db))))]
                     {:label (str/capitalize (name property-type))
                      :on-click (fn []
                                  ((requiring-resolve 'cdq.ui.editor/open-main-window!) property-type))})}]
   :update-labels [{:label "Mouseover-entity id"
                    :update-fn (fn []
                                 (when-let [entity (and ctx/mouseover-eid @ctx/mouseover-eid)]
                                   (:entity/id entity)))
                    :icon (ctx/assets "images/mouseover.png")}
                   {:label "elapsed-time"
                    :update-fn (fn [] (str (readable-number ctx/elapsed-time) " seconds"))
                    :icon (ctx/assets "images/clock.png")}
                   {:label "paused?"
                    :update-fn (fn [] ctx/paused?)}
                   {:label "GUI"
                    :update-fn (fn [] (graphics/mouse-position ctx/graphics))}
                   {:label "World"
                    :update-fn (fn [] (mapv int (graphics/world-mouse-position ctx/graphics)))}
                   {:label "Zoom"
                    :update-fn (fn [] (camera/zoom (:camera (:world-viewport ctx/graphics))))
                    :icon (ctx/assets "images/zoom.png")}
                   {:label "FPS"
                    :update-fn (fn [] (gdx.graphics/frames-per-second gdx/graphics))
                    :icon (ctx/assets "images/fps.png")}]})

(defn- player-state-actor []
  (actor/create {:draw (fn [_this] (state/draw-gui-view (entity/state-obj @ctx/player-eid)))}))

(defn- create-actors []
  ; TODO or I pass 'dev-menu-impl
  ; 'action-bar-impl'
  ; 'hp-mana-bar'
  ; 'entity-info-window'
  ; 'inventory-window'
  ; 'player-state-actor'
  ; 'player-message-actor'
  ; => as protocols?
  [(ui.menu/create (dev-menu-config))
   (action-bar/create)
   (hp-mana-bar/create [(/ (:width (:ui-viewport ctx/graphics)) 2)
                        80 ; action-bar-icon-size
                        ])
   (ui/group {:id :windows
              :actors [(entity-info-window/create [(:width (:ui-viewport ctx/graphics)) 0])
                       (inventory-window/create [(:width  (:ui-viewport ctx/graphics))
                                                 (:height (:ui-viewport ctx/graphics))])]})
   (player-state-actor)
   (player-message/create)])

(defn create! []
  (ui/load! {:skin-scale :x1} #_(:vis-ui config))
  (let [stage (stage/create (:ui-viewport ctx/graphics)
                            (:batch       ctx/graphics))]
    (run! (partial stage/add-actor! stage) (create-actors))
    (input/set-processor! gdx/input stage)
    stage))

; (viewport/unproject-mouse-position (stage/viewport stage))
; => move ui-viewport inside stage?
; => viewport/unproject-mouse-position ? -> already exists!
; => stage/resize-viewport! need to add (for viewport)
(defn mouse-on-actor? [stage]
  (stage/hit stage (graphics/mouse-position ctx/graphics)))
