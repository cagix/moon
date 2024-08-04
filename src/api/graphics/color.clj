(ns api.graphics.color
  (:require [utils.reflect :refer [bind-roots]]))

(declare black
         blue
         brown
         chartreuse
         clear
         coral
         cyan
         dark-gray
         firebrick
         forest
         gold
         goldenrod
         gray
         green
         light-gray
         lime
         magenta
         maroon
         navy
         olive
         orange
         pink
         purple
         red
         royal
         salmon
         scarlet
         sky
         slate
         tan
         teal
         violet
         white
         yellow)

(bind-roots "com.badlogic.gdx.graphics.Color"
            'com.badlogic.gdx.graphics.Color
            "api.graphics.color")
