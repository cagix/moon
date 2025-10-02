(ns clojure.scene2d.vis-ui.window
  (:require [gdl.scene2d.actor :as actor]
            [clojure.scene2d.vis-ui.table :as table]
            [com.kotcrab.vis.ui.widget.vis-window :as vis-window])
  (:import (com.badlogic.gdx.scenes.scene2d.ui Label
                                               Window)))

(defn create
  [{:keys [title
           modal?
           close-button?
           center?
           close-on-escape?]
    :as opts}]
  (let [window (vis-window/create
                {:title title
                 :close-button? close-button?
                 :center? center?
                 :close-on-escape? close-on-escape?
                 :show-window-border? true})]
    (.setModal window (boolean modal?))
    (table/set-opts! window opts)))

; TODO buggy FIXME
(defn title-bar?
  "Returns true if the actor is a window title bar."
  [actor]
  (when (instance? Label actor)
    (when-let [p (actor/parent actor)]
      (when-let [p (actor/parent p)]
        (and (instance? Window actor)
             (= (.getTitleLabel ^Window p) actor))))))
