(ns cdq.ui.message
  (:require [cdq.graphics :as graphics]
            [clojure.gdx.scene2d.actor :as actor]
            [clojure.gdx.scene2d.stage :as stage]
            [clojure.gdx.utils.viewport :as viewport]))

(defn- draw-message [state vp-width vp-height]
  (when-let [text (:text @state)]
    [:draw/text {:x (/ vp-width 2)
                 :y (+ (/ vp-height 2) 200)
                 :text text
                 :scale 2.5
                 :up? true}]))

; cdq.ui.actor with optional draw/act and passing ctx !

(defn create [duration-seconds]
  (doto (actor/create
         {:draw (fn [this _batch _parent-alpha]
                  (when-let [stage (actor/stage this)]
                    (graphics/draw! (:ctx/graphics (.ctx stage))
                                    [(draw-message (actor/user-object this)
                                                   (viewport/world-width  (stage/viewport stage))
                                                   (viewport/world-height (stage/viewport stage)))])))
          :act (fn [this delta]
                 (let [state (actor/user-object this)]
                   (when (:text @state)
                     (swap! state update :counter + delta)
                     (when (>= (:counter @state) duration-seconds)
                       (reset! state nil)))))})
    (actor/set-name! "player-message")
    (actor/set-user-object! (atom nil))))

(defn show! [this text]
  (actor/set-user-object! this (atom {:text text
                                      :counter 0})))
