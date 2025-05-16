(ns cdq.impl.stage
  (:require [cdq.ctx :as ctx]
            [cdq.draw :as draw]
            [cdq.entity :as entity]
            [cdq.graphics :as graphics]
            [cdq.info :as info]
            [cdq.stage :refer [add-actor!] :as stage]
            [cdq.state :as state]
            [cdq.ui]
            [cdq.ui.action-bar]
            [cdq.ui.inventory]
            [cdq.utils :as utils]
            [cdq.val-max :as val-max]
            [clojure.string :as str]
            [gdl.graphics]
            [gdl.graphics.camera :as camera]
            [gdl.graphics.viewport :as viewport]
            [gdl.input :as input]
            [gdl.ui :as ui]
            [gdl.ui.actor :as actor])
  (:import (clojure.lang ILookup)
           (com.badlogic.gdx.scenes.scene2d Actor
                                            Group
                                            Stage)
           (com.badlogic.gdx.scenes.scene2d.ui Label
                                               Button
                                               Window)
           (com.kotcrab.vis.ui.widget VisWindow)))

(let [disallowed-keys [:entity/skills
                       #_:entity/fsm
                       :entity/faction
                       :active-skill]]
  (defn- ->label-text []
    ; items then have 2x pretty-name
    #_(.setText (.getTitleLabel window)
                (if-let [eid ctx/mouseover-eid]
                  (info/text [:property/pretty-name (:property/pretty-name @eid)])
                  "Entity Info"))
    (when-let [eid ctx/mouseover-eid]
      (info/text ; don't use select-keys as it loses Entity record type
                 (apply dissoc @eid disallowed-keys)))))

(defn- entity-info-window [position]
  (let [label (ui/label "")
        window (ui/window {:title "Info"
                           :id :entity-info-window
                           :visible? false
                           :position position
                           :rows [[{:actor label :expand? true}]]})]
    ; do not change window size ... -> no need to invalidate layout, set the whole stage up again
    ; => fix size somehow.
    (.addActor window (proxy [Actor] []
                        (act [_delta]
                          (.setText label (str (->label-text)))
                          (.pack window))))
    window))

(defn- render-infostr-on-bar [infostr x y h]
  (draw/text {:text infostr
              :x (+ x 75)
              :y (+ y 2)
              :up? true}))

(defn- hp-mana-bar [[x y-mana]]
  (let [rahmen      (graphics/sprite (ctx/assets "images/rahmen.png"))
        hpcontent   (graphics/sprite (ctx/assets "images/hp.png"))
        manacontent (graphics/sprite (ctx/assets "images/mana.png"))
        [rahmenw rahmenh] (:pixel-dimensions rahmen)
        y-hp (+ y-mana rahmenh)
        render-hpmana-bar (fn [x y contentimage minmaxval name]
                            (draw/image rahmen [x y])
                            (draw/image (graphics/sub-sprite contentimage
                                                             [0 0 (* rahmenw (val-max/ratio minmaxval)) rahmenh])
                                        [x y])
                            (render-infostr-on-bar (str (utils/readable-number (minmaxval 0)) "/" (minmaxval 1) " " name) x y rahmenh))]
    (proxy [Actor] []
      (draw [_batch _parent-alpha]
        (let [player-entity @ctx/player-eid
              x (- x (/ rahmenw 2))]
          (render-hpmana-bar x y-hp   hpcontent   (entity/hitpoints player-entity) "HP")
          (render-hpmana-bar x y-mana manacontent (entity/mana      player-entity) "MP"))))))

;"Mouseover-Actor: "
#_(when-let [actor (mouse-on-actor? ctx/stage)]
    (str "TRUE - name:" (.getName actor)
         "id: " (user-object actor)))

(defn- dev-menu-config []
  {:menus [{:label "World"
            :items (for [world-fn '[cdq.level.vampire/create
                                    cdq.level.uf-caves/create
                                    cdq.level.modules/create]]
                     {:label (str "Start " (namespace world-fn))
                      :on-click (fn [] ((requiring-resolve 'cdq.game.reset/do!) world-fn))})}
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
                    :update-fn (fn [] (gdl.graphics/frames-per-second))
                    :icon (ctx/assets "images/fps.png")}]})

(defn- player-state-actor []
  (proxy [Actor] []
    (draw [_batch _parent-alpha]
      (state/draw-gui-view (entity/state-obj @ctx/player-eid)))))

(defn- player-message []
  (doto (proxy [Actor] []
          (draw [_batch _parent-alpha]
            (let [state (Actor/.getUserObject this)]
              (when-let [text (:text @state)]
                (draw/text {:x (/ (:width     ctx/ui-viewport) 2)
                            :y (+ (/ (:height ctx/ui-viewport) 2) 200)
                            :text text
                            :scale 2.5
                            :up? true}))))
          (act [delta]
            (let [state (Actor/.getUserObject this)]
              (when (:text @state)
                (swap! state update :counter + delta)
                (when (>= (:counter @state) 1.5)
                  (reset! state nil))))))
    (.setUserObject (atom nil))
    (.setName "player-message-actor")))

(defn show-message! [stage text]
  (Actor/.setUserObject (Group/.findActor (stage/root stage) "player-message-actor")
                        (atom {:text text
                               :counter 0})))

(defn- check-escape-close-windows [windows]
  (when (input/key-just-pressed? :escape)
    (run! #(Actor/.setVisible % false) (Group/.getChildren windows))))

(def window-hotkeys {:inventory-window  :i
                     :entity-info-window :e})

(defn- check-window-hotkeys [windows]
  (doseq [[id input-key] window-hotkeys
          :when (input/key-just-pressed? input-key)]
    (actor/toggle-visible! (get windows id))))

(defn- create-actors []
  [(cdq.ui/menu (dev-menu-config))
   (cdq.ui.action-bar/create)
   (hp-mana-bar [(/ (:width ctx/ui-viewport) 2)
                 80 ; action-bar-icon-size
                 ])
   (ui/group {:id :windows
              :actors [(proxy [Actor] []
                         (act [_delta]
                           (check-window-hotkeys       (Actor/.getParent this))
                           (check-escape-close-windows (Actor/.getParent this))))
                       (entity-info-window [(:width ctx/ui-viewport) 0])
                       (cdq.ui.inventory/create [(:width  ctx/ui-viewport)
                                                 (:height ctx/ui-viewport)])]})
   (player-state-actor)
   (player-message)])

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
                                                    (Actor/.remove (::modal stage))
                                                    (on-click)))]]
                          :id ::modal
                          :modal? true
                          :center-position [(/ (:width  ctx/ui-viewport) 2)
                                            (* (:height ctx/ui-viewport) (/ 3 4))]
                          :pack? true})))

(defn inventory-visible? [stage]
  (-> stage :windows :inventory-window Actor/.isVisible))

(defn inventory-cell-with-item? [actor]
  {:pre [actor]}
  (and (Actor/.getParent actor)
       (= "inventory-cell" (Actor/.getName (Actor/.getParent actor)))
       (get-in (:entity/inventory @ctx/player-eid)
               (Actor/.getUserObject (Actor/.getParent actor)))))

(defn window-title-bar? ; TODO buggy FIXME
  "Returns true if the actor is a window title bar."
  [^Actor actor]
  (when (instance? Label actor)
    (when-let [p (.getParent actor)]
      (when-let [p (.getParent p)]
        (and (instance? VisWindow actor)
             (= (.getTitleLabel ^Window p) actor))))))

(defn- button-class? [actor]
  (some #(= Button %) (supers (class actor))))

(defn button?
  "Returns true if the actor or its parent is a button."
  [^Actor actor]
  (or (button-class? actor)
      (and (.getParent actor)
           (button-class? (.getParent actor)))))

(defn create! []
  (ui/load! ctx/ui-config)
  (let [stage (Stage. (:java-object ctx/ui-viewport)
                      (:java-object ctx/batch))]
    (run! #(.addActor stage %) (create-actors))
    (input/set-processor! stage)
    (reify
      ILookup
      (valAt [_ id]
        (ui/find-actor-with-id (.getRoot stage) id))

      (valAt [_ id not-found]
        (or (ui/find-actor-with-id (.getRoot stage) id)
            not-found))

      cdq.stage/Stage
      (add-actor! [_ actor]
        (.addActor stage actor))

      (draw! [_]
        (.draw stage))

      (act! [_]
        (.act stage))

      (mouse-on-actor? [_]
        (let [[x y] (viewport/mouse-position ctx/ui-viewport)]
          (.hit stage x y true)))

      (root [_]
        (.getRoot stage))

  (set-item! [stage cell item]
    (cdq.ui.inventory/set-item! stage cell item))

  (remove-item! [stage cell]
    (cdq.ui.inventory/remove-item! stage cell))

  (selected-skill [stage]
    (cdq.ui.action-bar/selected-skill stage))

  (add-skill! [stage skill]
    (cdq.ui.action-bar/add-skill! stage skill))

  (remove-skill! [stage skill]
    (cdq.ui.action-bar/remove-skill! stage skill))

  (show-message! [stage text]
    (show-message! stage text))

  (show-modal! [stage opts]
    (show-modal! stage opts))

  (inventory-visible? [stage]
    (inventory-visible? stage))

  (inventory-cell-with-item? [_ actor]
    (inventory-cell-with-item? actor))

  (window-title-bar? [_ actor]
    (window-title-bar? actor))

  (button? [_ actor]
    (button? actor)))))
