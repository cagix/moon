(ns cdq.start.provide-impls
  (:require clojure.provide))

(defn java-class? [s]
  (boolean (re-matches #".*\.[A-Z][A-Za-z0-9_]*" s)))

(comment
 (= (java-class? "cdq.db.DB")
    true)
 (= (java-class? "cdq.db/DB")
    false)
 (= (java-class? "cdq.db")
    false)

 )

(require 'cdq.gdx.graphics)
(require 'cdq.db)

(defn do! [ctx]
  (clojure.provide/do!
   '[
     [cdq.db.DB
      cdq.db
      cdq.ctx.db/DB]

     [com.badlogic.gdx.scenes.scene2d.Actor
      clojure.gdx.scene2d.actor
      clojure.scene2d.actor/Actor]

     [com.badlogic.gdx.scenes.scene2d.Group
      clojure.gdx.scene2d.group
      clojure.scene2d.group/Group]

     [com.badlogic.gdx.scenes.scene2d.Event
      clojure.gdx.scene2d.event
      clojure.scene2d.event/Event]

     [com.badlogic.gdx.scenes.scene2d.Stage
      clojure.gdx.scene2d.stage
      clojure.scene2d.stage/Stage]

     [com.badlogic.gdx.scenes.scene2d.Stage
      cdq.gdx.stage
      cdq.ctx.stage/Stage]

     [cdq.gdx.graphics.RGraphics
      cdq.gdx.graphics
      cdq.ctx.graphics/Graphics]

     [com.badlogic.gdx.scenes.scene2d.ui.Table
      clojure.gdx.scene2d.ui.table
      clojure.scene2d.ui.table/Table]
     ])
  ctx)
