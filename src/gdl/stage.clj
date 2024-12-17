(ns gdl.stage
  (:refer-clojure :exclude [get])
  (:require [clojure.gdx :as gdx]
            [gdl.graphics :as g]
            [gdl.ui :as ui]
            [gdl.ui.actor :as actor]
            [gdl.ui.group :as group]
            [gdl.utils :refer [pretty-pst with-err-str bind-root]])
  (:import (com.badlogic.gdx.scenes.scene2d Actor Stage)
           (com.badlogic.gdx.scenes.scene2d.ui Table ButtonGroup)))

(defn- stage* [viewport batch actors]
  (let [stage (proxy [Stage clojure.lang.ILookup] [viewport batch]
                (valAt
                  ([id]
                   (group/find-actor-with-id (.getRoot this) id))
                  ([id not-found]
                   (or (group/find-actor-with-id (.getRoot this) id)
                       not-found))))]
    (run! #(.addActor stage %) actors)
    stage))

(defn setup
  ([]
   (setup nil))
  ([actors]
   (def this (stage* g/viewport g/batch actors))
   (gdx/set-input-processor this)))

(defn cleanup []
  (.dispose this))

(defn act []
  (.act this))

(defn render []
  (.draw this))

(defn get []
  this)

(defn get-inventory []
  (clojure.core/get (:windows (get)) :inventory-window))

(defn get-action-bar []
  (let [group (:ui/action-bar (:action-bar-table (get)))]
    {:horizontal-group group
     :button-group (actor/user-object (group/find-actor group "action-bar/button-group"))}))

(defn selected-skill []
  (when-let [skill-button (ButtonGroup/.getChecked (:button-group (get-action-bar)))]
    (actor/user-object skill-button)))

(defn add-actor [actor]
  (.addActor (get) actor))

(defn reset [new-actors]
  (.clear (get))
  (run! add-actor new-actors))

(defn mouse-on-actor? []
  (let [[x y] (g/mouse-position)]
    (.hit (get) x y true)))

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
               :center-position [(/ g/viewport-width 2)
                                 (* g/viewport-height (/ 3 4))]
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
