(ns cdq.ui.player-message
  (:require [cdq.ctx :as ctx]
            [cdq.graphics :as graphics]
            [clojure.gdx :as gdx]
            [clojure.gdx.graphics :as gdx.graphics]
            [clojure.gdx.scene2d.actor :as actor]
            [clojure.gdx.scene2d.group :as group]
            [clojure.gdx.scene2d.stage :as stage]))

(defn show-message! [stage text]
  (actor/set-user-object! (group/find-actor (stage/root stage) "player-message-actor")
                          (atom {:text text
                                 :counter 0})))

(defn create []
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
