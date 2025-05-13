(ns cdq.stage
  (:require [cdq.ctx :as ctx]
            [cdq.entity :as entity]
            [cdq.entity.state :as state]
            [cdq.graphics :as graphics]
            [cdq.ui.action-bar :as action-bar]
            [cdq.ui.entity-info-window :as entity-info-window]
            [cdq.ui.inventory-window :as inventory-window]
            [cdq.ui.hp-mana-bar :as hp-mana-bar]
            [clojure.gdx :as gdx]
            [clojure.gdx.input :as input]
            [clojure.gdx.scene2d.actor :as actor]
            [clojure.gdx.scene2d.group :as group]
            [clojure.gdx.scene2d.ui :as ui]
            [clojure.gdx.scene2d.ui.menu :as ui.menu]
            [clojure.gdx.graphics :as gdx.graphics]
            [clojure.gdx.graphics.camera :as camera]
            [clojure.string :as str]
            [clojure.utils :as utils])
  (:import (clojure.lang ILookup)
           (com.badlogic.gdx.scenes.scene2d Stage)))

(defn root [^Stage stage]
  (Stage/.getRoot stage))

(defn add-actor! [^Stage stage actor]
  (.addActor stage actor))

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
                    :update-fn (fn [] (str (utils/readable-number ctx/elapsed-time) " seconds"))
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

(defn add-skill! [stage skill]
  (action-bar/add-skill! stage skill))

(defn remove-skill! [stage skill]
  (action-bar/remove-skill! stage skill))

(defn selected-skill [stage]
  (action-bar/selected-skill stage))

(defn set-item! [stage inventory-cell item]
  (inventory-window/set-item-image! stage inventory-cell item))

(defn remove-item! [stage inventory-cell]
  (inventory-window/remove-item-image! stage inventory-cell))

(defn- player-state-actor []
  (actor/create {:draw (fn [_this] (state/draw-gui-view (entity/state-obj @ctx/player-eid)))}))

(defn- player-message []
  (doto (actor/create {:draw (fn [this]
                               (let [g ctx/graphics
                                     state (actor/user-object this)]
                                 (when-let [text (:text @state)]
                                   (graphics/draw-text g {:x (/ (:width     (:ui-viewport g)) 2)
                                                          :y (+ (/ (:height (:ui-viewport g)) 2) 200)
                                                          :text text
                                                          :scale 2.5
                                                          :up? true}))))
                       :act (fn [this]
                              (let [state (actor/user-object this)]
                                (when (:text @state)
                                  (swap! state update :counter + (gdx.graphics/delta-time gdx/graphics))
                                  (when (>= (:counter @state) 1.5)
                                    (reset! state nil)))))})
    (actor/set-user-object! (atom nil))
    (actor/set-name! "player-message-actor")))

(defn show-message! [stage text]
  (actor/set-user-object! (group/find-actor (root stage) "player-message-actor")
                          (atom {:text text
                                 :counter 0})))

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
   (player-message)])

(defn create! []
  (ui/load! {:skin-scale :x1} #_(:vis-ui config))
  (let [stage (proxy [Stage ILookup] [(:ui-viewport ctx/graphics)
                                      (:batch       ctx/graphics)]
                (valAt
                  ([id]
                   (group/find-actor-with-id (root this) id))
                  ([id not-found]
                   (or (group/find-actor-with-id (root this) id)
                       not-found))))]
    (run! (partial add-actor! stage) (create-actors))
    (input/set-processor! gdx/input stage)
    stage))

(defn draw! [^Stage stage]
  (.draw stage))

(defn act! [^Stage stage]
  (.act stage))

; (viewport/unproject-mouse-position (stage/viewport stage))
; => move ui-viewport inside stage?
; => viewport/unproject-mouse-position ? -> already exists!
; => stage/resize-viewport! need to add (for viewport)
(defn mouse-on-actor? [^Stage stage]
  (let [[x y] (graphics/mouse-position ctx/graphics)]
    (.hit stage x y true)))

; no window movable type cursor appears here like in player idle
; inventory still working, other stuff not, because custom listener to keypresses ? use actor listeners?
; => input events handling
; hmmm interesting ... can disable @ item in cursor  / moving / etc.
(defn show-modal! [stage {:keys [title text button-text on-click]}]
  (assert (not (::modal stage)))
  (add-actor! stage
              (ui/window {:title title
                          :rows [[(ui/label text)]
                                 [(ui/text-button button-text
                                                  (fn []
                                                    (actor/remove! (::modal stage))
                                                    (on-click)))]]
                          :id ::modal
                          :modal? true
                          :center-position [(/ (:width  (:ui-viewport ctx/graphics)) 2)
                                            (* (:height (:ui-viewport ctx/graphics)) (/ 3 4))]
                          :pack? true})))

(defn show-error-window! [stage throwable]
  (add-actor! stage
              (ui/window {:title "Error"
                          :rows [[(ui/label (binding [*print-level* 3]
                                              (utils/with-err-str
                                                (clojure.repl/pst throwable))))]]
                          :modal? true
                          :close-button? true
                          :close-on-escape? true
                          :center? true
                          :pack? true})))

(defn check-window-controls! [stage]
  (let [window-hotkeys {:inventory-window   :i
                        :entity-info-window :e}]
    (doseq [window-id [:inventory-window
                       :entity-info-window]
            :when (input/key-just-pressed? gdx/input (get window-hotkeys window-id))]
      (actor/toggle-visible! (get (:windows stage) window-id))))
  (when (input/key-just-pressed? gdx/input :escape)
    (let [windows (group/children (:windows stage))]
      (when (some actor/visible? windows)
        (run! #(actor/set-visible! % false) windows)))))

(defn inventory-visible? [stage]
  (-> stage :windows :inventory-window actor/visible?))

(defn toggle-inventory-visible! [stage]
  (-> stage :windows :inventory-window actor/toggle-visible!))
