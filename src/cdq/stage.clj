(ns cdq.stage
  (:require [cdq.entity :as entity]
            [cdq.info :as info]
            [cdq.val-max :as val-max]
            cdq.widgets.inventory
            [cdq.graphics :as graphics]
            [cdq.graphics.sprite :as sprite]
            [cdq.graphics.batch :as batch]
            [cdq.graphics.text :as text]
            [cdq.ui :as ui :refer [ui-actor]]
            [cdq.ui.actor :as actor]
            [cdq.ui.group :as group]
            [cdq.utils :as utils]))

(defn- action-bar-button-group []
  (let [actor (ui-actor {})]
    (.setName actor "action-bar/button-group")
    (actor/set-user-object actor (ui/button-group {:max-check-count 1
                                                   :min-check-count 0}))
    actor))

(defn- action-bar* []
  (let [group (ui/horizontal-group {:pad 2 :space 2})]
    (.setUserObject group :ui/action-bar)
    (group/add-actor! group (action-bar-button-group))
    group))

(defn- action-bar []
  (ui/table {:rows [[{:actor (action-bar*)
                      :expand? true
                      :bottom? true}]]
             :id :action-bar-table
             :cell-defaults {:pad 2}
             :fill-parent? true}))

(def ^:private disallowed-keys [:entity/skills
                                #_:entity/fsm
                                :entity/faction
                                :active-skill])

(defn- ->label-text [{:keys [cdq.context/mouseover-eid] :as c}]
  ; items then have 2x pretty-name
  #_(.setText (.getTitleLabel window)
              (if-let [eid mouseover-eid]
                (info/text c [:property/pretty-name (:property/pretty-name @eid)])
                "Entity Info"))
  (when-let [eid mouseover-eid]
    (info/text c ; don't use select-keys as it loses Entity record type
               (apply dissoc @eid disallowed-keys))))

(defn- entity-info-window [position]
  (let [label (ui/label "")
        window (ui/window {:title "Info"
                           :id :entity-info-window
                           :visible? false
                           :position position
                           :rows [[{:actor label :expand? true}]]})]
    ; do not change window size ... -> no need to invalidate layout, set the whole stage up again
    ; => fix size somehow.
    (group/add-actor! window (ui-actor {:act (fn [context]
                                               (.setText label (str (->label-text context)))
                                               (.pack window))}))
    window))

(defn- render-infostr-on-bar [c infostr x y h]
  (text/draw c
             {:text infostr
              :x (+ x 75)
              :y (+ y 2)
              :up? true}))

(defn- hp-mana-bar [context [x y-mana]]
  (let [rahmen      (sprite/create context "images/rahmen.png")
        hpcontent   (sprite/create context "images/hp.png")
        manacontent (sprite/create context "images/mana.png")
        [rahmenw rahmenh] (:pixel-dimensions rahmen)
        y-hp (+ y-mana rahmenh)
        render-hpmana-bar (fn [c x y contentimage minmaxval name]
                            (batch/draw-image c rahmen [x y])
                            (batch/draw-image c
                                              (sprite/sub contentimage
                                                          [0 0 (* rahmenw (val-max/ratio minmaxval)) rahmenh]
                                                          c)
                                              [x y])
                            (render-infostr-on-bar c (str (utils/readable-number (minmaxval 0)) "/" (minmaxval 1) " " name) x y rahmenh))]
    (ui-actor {:draw (fn [{:keys [cdq.context/player-eid] :as c}]
                       (let [player-entity @player-eid
                             x (- x (/ rahmenw 2))]
                         (render-hpmana-bar c x y-hp   hpcontent   (entity/hitpoints   player-entity) "HP")
                         (render-hpmana-bar c x y-mana manacontent (entity/mana        player-entity) "MP")))})))

(defn- draw-player-message [{:keys [cdq.graphics/ui-viewport
                                    cdq.context/player-message] :as c}]
  (when-let [text (:text @player-message)]
    (text/draw c
               {:x (/ (:width ui-viewport) 2)
                :y (+ (/ (:height ui-viewport) 2) 200)
                :text text
                :scale 2.5
                :up? true})))

(defn- check-remove-message [{:keys [cdq.context/player-message]}]
  (when (:text @player-message)
    (swap! player-message update :counter + (graphics/delta-time))
    (when (>= (:counter @player-message)
              (:duration-seconds @player-message))
      (swap! player-message dissoc :counter :text))))

(defn- player-message-actor []
  (ui-actor {:draw draw-player-message
             :act  check-remove-message}))

(defn- player-state-actor []
  (ui-actor {:draw #(entity/draw-gui-view (entity/state-obj @(:cdq.context/player-eid %))
                                          %)}))

(defn- window-group [context actors]
  (ui/group {:id :windows
             :actors actors}))

(defn actors [{:keys [cdq.graphics/ui-viewport] :as context}]
  [((requiring-resolve 'cdq.impl.ui.dev-menu/create) context) ; requires cdq.world.context
   (action-bar)
   (hp-mana-bar context [(/ (:width ui-viewport) 2)
                         80 ; action-bar-icon-size
                         ])
   (window-group context [(entity-info-window [(:width ui-viewport) 0])
                          (cdq.widgets.inventory/create context [(:width  ui-viewport)
                                                                 (:height ui-viewport)])])
   (player-state-actor)
   (player-message-actor)])
