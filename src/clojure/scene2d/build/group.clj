(ns clojure.scene2d.build.group
  (:require [clojure.scene2d :as scene2d]
            [clojure.scene2d.build.actor :as actor]
            [com.badlogic.gdx.scenes.scene2d.group :as group])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)))

(defn set-opts! [group opts]
  (run! (fn [actor-or-decl]
          (group/add-actor! group
                            (if (instance? Actor actor-or-decl)
                              actor-or-decl
                              (scene2d/build actor-or-decl))))
        (:group/actors opts))
  (actor/set-opts! group opts))

(defmethod scene2d/build :actor.type/group [opts]
  (doto (group/create)
    (set-opts! opts)))
