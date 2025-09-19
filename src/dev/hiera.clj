(ns dev.hiera
  (:require [hiera.main :as hiera]
            [clojure.set :as set]))

; A place for everything and everything in its place
; e.g. 'impl' for components
; and 'api' for stuff
; the api should be minimal
; whats the badlogic API?
; whats hidden ? (converters) ?

(def good-enough
  '#{
     cdq.application
     cdq.ctx
     cdq.db
     cdq.entity.state
     cdq.effect
     cdq.graphics
     cdq.schema
     cdq.stage
     cdq.creature
     cdq.malli
     cdq.string
     cdq.stats
     cdq.input
     cdq.position
     cdq.world
     cdq.world.content-grid
     cdq.world.grid
     cdq.body
     cdq.entity
     cdq.timer

     clojure.rand
     clojure.utils

     gdl

     com.badlogic.gdx.backends.lwjgl3
     com.badlogic.gdx.graphics.color
     com.badlogic.gdx.scenes.scene2d.ui.cell
     com.badlogic.gdx.utils.align

     cdq.ui.dev-menu
     })

; We have many 'namespaces' which are actually _not_ an API
; e.g. entity create/tick/draw is extending something ....
; or vis-ui shit
; requiring-resolve ?

(comment

 ; java heap space 512m required
 (hiera/graph
  {:sources #{"src"}
   :output "target/hiera"
   :layout :horizontal
   :external false
   :ignore good-enough})

 )
