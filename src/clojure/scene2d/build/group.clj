(ns clojure.scene2d.build.group
  (:require [clojure.scene2d :as scene2d]
            [clojure.scene2d.actor :as actor]
            [clojure.scene2d.group :as pgroup]
            [com.badlogic.gdx.scenes.scene2d.group :as group])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)))

(extend-type com.badlogic.gdx.scenes.scene2d.Group
  pgroup/Group
  (add! [group actor]
    (group/add-actor! group actor))

  (find-actor [group name]
    (group/find-actor group name))

  (clear-children! [group]
    (group/clear-children! group))

  (children [group]
    (group/children group))

  (set-opts! [group opts]
    (run! (fn [actor-or-decl]
            (pgroup/add! group (if (instance? Actor actor-or-decl)
                                 actor-or-decl
                                 (scene2d/build actor-or-decl))))
          (:group/actors opts))
    (actor/set-opts! group opts)))

(defmethod scene2d/build :actor.type/group [opts]
  (doto (group/create)
    (pgroup/set-opts! opts)))
