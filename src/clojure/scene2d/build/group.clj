(ns clojure.scene2d.build.group
  (:require [clojure.scene2d :as scene2d]
            [clojure.scene2d.build.actor :as actor])
  (:import (com.badlogic.gdx.scenes.scene2d Actor
                                            Group)))

(defn set-opts! [group opts]
  (run! (fn [actor-or-decl]
          (Group/.addActor group
                           (if (instance? Actor actor-or-decl)
                             actor-or-decl
                             (scene2d/build actor-or-decl))))
        (:group/actors opts))
  (actor/set-opts! group opts))

(defmethod scene2d/build :actor.type/group [opts]
  (doto (Group.)
    (set-opts! opts)))
