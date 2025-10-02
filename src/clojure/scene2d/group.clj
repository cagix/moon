(ns clojure.scene2d.group
  (:require [com.badlogic.gdx.scenes.scene2d :as scene2d]
            [com.badlogic.gdx.scenes.scene2d.actor :as actor]
            [com.badlogic.gdx.scenes.scene2d.group :as group]))

(defn set-opts! [group opts]
  (run! (fn [actor-or-decl]
          (group/add! group (if (instance? com.badlogic.gdx.scenes.scene2d.Actor actor-or-decl)
                              actor-or-decl
                              (scene2d/build actor-or-decl))))
        (:group/actors opts))
  (actor/set-opts! group opts))
