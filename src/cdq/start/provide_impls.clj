(ns cdq.start.provide-impls
  (:require clojure.provide))

(require 'cdq.gdx.graphics)

(defn do! [ctx]
  (clojure.provide/do!
   [
    [com.badlogic.gdx.scenes.scene2d.Actor
     'clojure.gdx.scene2d.actor
     @(requiring-resolve 'clojure.scene2d.actor/Actor)]

    [com.badlogic.gdx.scenes.scene2d.Group
     'clojure.gdx.scene2d.group
     @(requiring-resolve 'clojure.scene2d.group/Group)]

    [com.badlogic.gdx.scenes.scene2d.Event
     'clojure.gdx.scene2d.event
     @(requiring-resolve 'clojure.scene2d.event/Event)]

    [com.badlogic.gdx.scenes.scene2d.Stage
     'clojure.gdx.scene2d.stage
     @(requiring-resolve 'clojure.scene2d.stage/Stage)]

    [com.badlogic.gdx.scenes.scene2d.Stage
     'cdq.gdx.stage
     @(requiring-resolve 'cdq.ctx.stage/Stage)]

    [cdq.gdx.graphics.RGraphics
     'cdq.gdx.graphics
     @(requiring-resolve 'cdq.ctx.graphics/Graphics)]

    [com.badlogic.gdx.scenes.scene2d.ui.Table
     'clojure.gdx.scene2d.ui.table
     @(requiring-resolve 'clojure.scene2d.ui.table/Table)]
    ])
  ctx)


