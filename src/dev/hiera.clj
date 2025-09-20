(ns dev.hiera
  (:require [hiera.main :as hiera]
            [clojure.set :as set]))

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

(comment

 ; java heap space 512m required
 (hiera/graph
  {:sources #{"src"}
   :output "target/hiera"
   :layout :horizontal
   :external false
   ;:ignore good-enough

   })

 )
