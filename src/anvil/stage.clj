(ns anvil.stage
  (:refer-clojure :exclude [get])
  (:require [anvil.app :as app]
            [anvil.graphics :as g]
            [anvil.screen :as screen]
            [anvil.ui :as ui]
            [clojure.gdx.input :as input]
            [clojure.gdx.scene2d.group :refer [find-actor-with-id]]
            [clojure.gdx.scene2d.stage :as stage]
            [clojure.gdx.utils.disposable :refer [dispose]]
            [clojure.utils :refer [pretty-pst with-err-str bind-root]])
  (:import (com.badlogic.gdx.scenes.scene2d Actor Stage)))

(defn get []
  (:stage (app/current-screen)))

(defn add-actor [actor]
  (.addActor (get) actor))

(defn reset [new-actors]
  (.clear (get))
  (run! add-actor new-actors))

(defn mouse-on-actor? []
  (let [[x y] (g/gui-mouse-position)]
    (.hit (get) x y true)))

(defrecord StageScreen [stage sub-screen]
  screen/Screen
  (enter [_]
    (input/set-processor stage)
    (screen/enter sub-screen))

  (exit [_]
    (input/set-processor nil)
    (screen/exit sub-screen))

  (render [_]
    (stage/act stage)
    (screen/render sub-screen)
    (stage/draw stage))

  (dispose [_]
    (dispose stage)
    (screen/dispose sub-screen)))

(def ^:private empty-screen
  (reify screen/Screen
    (enter [_])
    (exit [_])
    (render [_])
    (dispose [_])))

(defn create
  "Actors or screen can be nil."
  [{:keys [actors screen]}]
  (let [stage (proxy [Stage clojure.lang.ILookup] [g/gui-viewport g/batch]
                (valAt
                  ([id]
                   (find-actor-with-id (stage/root this) id))
                  ([id not-found]
                   (or (find-actor-with-id (stage/root this) id)
                       not-found))))]
    (run! #(.addActor stage %) actors)
    (->StageScreen stage (or screen empty-screen))))

; no window movable type cursor appears here like in player idle
; inventory still working, other stuff not, because custom listener to keypresses ? use actor listeners?
; => input events handling
; hmmm interesting ... can disable @ item in cursor  / moving / etc.
(defn show-modal [{:keys [title text button-text on-click]}]
  (assert (not (::modal (get))))
  (add-actor
   (ui/window {:title title
               :rows [[(ui/label text)]
                      [(ui/text-button button-text
                                       (fn []
                                         (Actor/.remove (::modal (get)))
                                         (on-click)))]]
               :id ::modal
               :modal? true
               :center-position [(/ g/gui-viewport-width 2)
                                 (* g/gui-viewport-height (/ 3 4))]
               :pack? true})))



(defn error-window! [throwable]
  (pretty-pst throwable)
  (add-actor
   (ui/window {:title "Error"
               :rows [[(ui/label (binding [*print-level* 3]
                                   (with-err-str
                                     (clojure.repl/pst throwable))))]]
               :modal? true
               :close-button? true
               :close-on-escape? true
               :center? true
               :pack? true})))

(def player-message-duration-seconds 1.5)

(def message-to-player nil)

(defn show-player-msg [message]
  (bind-root message-to-player {:message message :counter 0}))
