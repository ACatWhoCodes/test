import java.util.*;

final int frame_rate = 60;

// number of communities
int num_communities = 1;

// confine_radius: the radius that particles have to stay within during quarantine
// flocking_dist: distance two particles have to be within to trigger flocking
int confine_radius = 5, flocking_dist = 50;
// the distance 2 particles have to be within to trigger social distancing
float repulsion_dist = 10;

// radius of the particles, used for collision detection, sometimes it's used in drawing
// recover_time: time needed for particle to recover; for each particle, the time count since infection is incremented every frame, once infected_time goes over recover_time, the particle recovers
float particle_radius = 10, recover_time = 1000;

// infection_radius: radius to particles have to be within to spread the virus
float infection_radius = 20;
float infection_radius_maximum = 50;

// infection_chance: chance of infection, a number from 0 to 1, on initial contact and every half second after
float default_infection_chance = 0.2;
float infection_chance_maximum = 1;
float chance_of_death = 0.5;

boolean showInfectionRadius = true;

boolean socialDistancingDefault = true;

// list of communities containing particles
Community[] communities;
int communitySize = 500;
int community_offset = 50;

//graph variables and array
int num = 50;
int lastInfected = 0;
int everyFrame = 100;
int goingUp = 0;

color arr = color(150, 0, 150);
color dea = color(150, 150, 0);
float[] array = new float[40];
float[] deadArray = new float[40];

// slider techincalities
int slider_length_y = 40;
int slider_offset_y = 100;

int lowest_edge = 0;
int rightmost_edge = 0;

// sliders
Slider infectionRadius;
Slider[] infectionChances;

// toggle buttons
toggleButton[] socialDistancingToggles;
toggleButton panningToggle;
toggleButton startButton;

// toggle button technicalities
float button_offset_y = 15;
int button_offset_x = 10;

int button_size_y = 30;
int button_size_x = 180;

// zooming and panning variables
float mx,my,ratio,xpt,ypt,xzt,yzt,swt,zoom;

// current relative origin: (xpt - xzt, ypt - yzt)
// current relative mouseX, mouseY: (xpt - xzt + mouseX, ypt - yzt + mouseY)

// this is only called once, at the beginning of the program
void setup() {
  // these numbers have to be hard-coded
  // width and height is the size of the entire screen, it is automatically set after calling size
  // size(num_communities * 600, 1500);
  size(1500, 1500);
  frameRate(frame_rate);

  // setting up zooming and panning variables
  zoom=1.0;
  mx=width/60;
  my=mx*height/width;
  ratio=mx/my;

  
  communities = new Community[num_communities];

  for(int i = 0; i < communities.length; i++){
    communities[i] = new Community((int)(((communitySize*1.5)*i)+community_offset),(int)(((communitySize*1.5)*i+communitySize)+community_offset),community_offset,communitySize,default_infection_chance,socialDistancingDefault);
  }

  infectionChances = new Slider[num_communities];

  socialDistancingToggles = new toggleButton[num_communities];

  startButton = new toggleButton("Start Simulation",false,40,25,false,0);
  panningToggle = new toggleButton("Panning",false,40,community_offset * 3 + communitySize,true,0);

  for (int i = 0; i < communities.length; i++) {
    Community c = communities[i];
    infectionChances[i] = new Slider("Chance of Infection", c.infection_chance,
        c.community_left,c.community_right, c.community_top, infection_chance_maximum);
    int tempY = c.community_top;
    int tempX = c.community_right;

    if (tempY > lowest_edge) {
      lowest_edge = tempY;
    }

    if (tempX > rightmost_edge) {
      rightmost_edge = tempX;
    }

    socialDistancingToggles[i] = new toggleButton("Social Distancing",c.socialDistancing,c.community_left,c.community_top, false,20);
  }

  infectionRadius = new Slider("Radius of Infection", infection_radius, 100, 
                               rightmost_edge-100,lowest_edge+slider_offset_y*6,infection_radius_maximum);

}

// randomly chooses a person and a community to travel to


// this is called every frame
void draw() { 
  background(177,162,150);

  if(!startButton.value){
    startButton.update();
  }
  
  // zooming and panning
  panningToggle.update();
  scale(zoom);
  translate(xpt-xzt,ypt-yzt);

  textSize(20);
  textAlign(LEFT);
  strokeWeight(2);
  stroke(0);
  
  if(startButton.value) {
    int total_currently_infected = 0;
    int total_currently_dead = 0;

    for (Community c : communities) {
      c.nextFrame();
      total_currently_infected += c.num_currently_infected;
      total_currently_dead += c.dead_particles.size();

      fill(0,0,0);
      
      if (c.num_recovered > 0) {
        c.R0 = round(c.overall_infected/c.num_recovered*100)/100.0;
      }
      c.num_susceptible = (int)(c.particles.size() - c.num_recovered - c.num_currently_infected + c.dead_particles.size());
      
      text("R{0} = " + c.R0, c.community_left,c.community_bottom - 5);
      text("R = " + c.R0 * c.num_susceptible, c.community_left + 120,c.community_bottom - 5);
    }
    
    //graph stuff
    noFill();
    line(610, 940, 610, 1245);
    line(610,1245,1200,1245);
    fill(1,1,1);
    text("300", 550, 950);
    text("0", 575, 1250);
    text("current", 1275, 1275);
    text("-60", 590, 1275);
    text("time (sec)", 825, 1300);
    if(frameCount % everyFrame == 0){
      for (int i=0; i<array.length-1; i++) {
        array[i] = array[i+1];
        deadArray[i] = deadArray[i+1];
      }
      float newValue = (float)total_currently_infected;
      float newValueDead = (float)total_currently_dead;

      array[array.length-1] = newValue;
      deadArray[array.length-1] = newValueDead;
    }
    graphing(array, arr);
    graphing(deadArray, dea);
    
    fill(0,0,0);
    //text("Current infected:        —  ", (width/2)-350, (height/2)-array[array.length-1]+500); 
    text("Current Infected: " + (int)total_currently_infected, 800, 850+500);
    text("People Dead " + (int)total_currently_dead, 800, 850+550);
  }

  // sliders and toggle buttons
  infectionRadius.update();
  infection_radius = infectionRadius.get();

  for(int i = 0; i < communities.length; i++){
    infectionChances[i].update();
    socialDistancingToggles[i].update();
    communities[i].infection_chance = infectionChances[i].get();
    communities[i].socialDistancing = socialDistancingToggles[i].value;
  }
}

// handling panning 
void mouseDragged() {
  if(panningToggle.value){
    xpt-=(pmouseX-mouseX)/zoom;
    ypt-=(pmouseY-mouseY)/zoom;
  }
}

// handling zooming
// accepts 1 or -1 for count
// 1 is zoom out, -1 is zoom in
void keyPressed() {
  int count;
  // see "https://keycode.info/" for getting character keycodes
  // if the user presses a
  if (keyCode == 65) count = 1;
  // if the user presses d
  else if (keyCode == 68) count = -1;
  else return;

  swt-=count;
  if (swt==0) {
    zoom=1.0;
  } else if (swt>=1 && swt<=10) {
    zoom=pow(2, swt);
  } else if (swt<=-1 && swt>=-10) {
    zoom=1/pow(2, abs(swt));
  }
  xzt=((zoom-1)*width/2)/zoom;
  yzt=((zoom-1)*height/2)/zoom;
  if(count<=-1){
    xpt-=(mouseX-width/2)/zoom;
    ypt-=(mouseY-height/2)/zoom;
  } else {
    xpt+=(mouseX-width/2)/(pow(2, swt+1));
    ypt+=(mouseY-height/2)/(pow(2, swt+1));
  }
}

class Community {
  float infection_chance, overall_infected, num_recovered, R0;
  boolean socialDistancing;

  // num_centers: number of party centers
  int num_centers = 2, num_currently_infected;

  // starting amount of particles
  int default_num_particles = 100, num_susceptible;
  // locations of the left and right and top and bottom of the community
  // note that process uses a different coordinate system
  // so (0, 0) is the top left corner
  // which mean that community_top will refer to the 
  int community_left, community_right, community_top, community_bottom;

  ArrayList<Particle> particles;
  ArrayList<Particle> dead_particles;

  // radius of centers
  int center_radius = 50;
  // location of centers
  PVector[] centers;

  Community(int com_left, int com_right, int com_bot, int com_top, float infect_chance, boolean socialDistancing) {
    infection_chance=infect_chance;
    this.socialDistancing = socialDistancing;
    num_recovered = 0;
    overall_infected = 0;
    R0 = 0;
    dead_particles = new ArrayList<Particle>();

    community_left = com_left; community_right = com_right;
    community_top = com_top; community_bottom = com_bot;
    centers = new PVector[num_centers];
    for (int i = 0; i < num_centers; i ++) {
      centers[i] = new PVector(random(community_left + center_radius, community_right - center_radius), random(community_bottom + center_radius, community_top - center_radius));
    }
    particles = new ArrayList<Particle>();
    for (int i = 0; i < default_num_particles; i ++) {
      float xVal = random(community_left + particle_radius, community_right - particle_radius);
      float yVal = random(community_bottom + particle_radius, community_top - particle_radius);
      particles.add(new Particle(new PVector(xVal, yVal), this));
    }
    // one unlucky person is the source of infection
    particles.get((int) random(particles.size())).state = 1;
  }

  void socialDistance(int i, int j) {
    // the difference vector between the 2 particles
    PVector diff = PVector.sub(particles.get(i).pos, particles.get(j).pos);
    // scale the vector so the attraction force is the same every time
    diff.normalize(); diff.div(10);

    // give particle i a little push away from particle j
    particles.get(i).applyForce(diff);
    // reverses the force
    diff.mult(-1);
    // give particle j a little push towards particle i
    particles.get(j).applyForce(diff);
  }
  void flocking(Particle p1, Particle p2) {
    // the difference vector between the 2 particles
    PVector diff = PVector.sub(p1.pos, p2.pos);
    // scale the vector so the attraction force is the same every time
    diff.normalize(); diff.div(12);
    // give particle j a little push towards particle i
    p2.applyForce(diff);
    // reverses the force
    diff.mult(-1);
    // give particle i a little push towards particle j
    p1.applyForce(diff);
  }
  void collisionCheck() {
    // check collision with other particles by considering them pair by pair
    // i: the first person in consideration
    for (int i = 0; i < particles.size(); i ++) {
      Particle p1 = particles.get(i);
      // j: the second person in consideration
      for (int j = 0; j < i; j ++) {
        Particle p2 = particles.get(j);
        // distance between person i and j
        float dis = p1.pos.dist(p2.pos);
        // social distance
        if (socialDistancing && dis < particle_radius * 2 + repulsion_dist) {
          socialDistance(i, j);
        }
        // flocking
        // if both gatherers and close enough
        else if (p1.gatherer && p2.gatherer && dis < flocking_dist) {  
          flocking(p1, p2);
        }
        // infection
        // if one is infected
        boolean p1_infected = p1.state == 1;
        boolean p2_infected = p2.state == 1;
        // if not a recovered patient
        boolean not_recovered = p1.state != 2 && p2.state != 2;
        // if close enough
        boolean close = dis < infection_radius;
        // if the two particles have failed to infect each other in the last half second
        boolean recently_failed = p1.recent_infect_fails.containsKey(p2);

        if(recently_failed) {
          // .replace() wasn't working in replit
          // current number of frames since last infection attempt
          int curr_val = p1.recent_infect_fails.get(p2);
          // increases number of frames since last infection attempt by 1
          p1.recent_infect_fails.remove(p2);
          p1.recent_infect_fails.put(p2, curr_val+1);

          // removes particle from recentInfectFails after half a second
          if(p1.recent_infect_fails.get(p2) >= (frame_rate/2)) {
            p1.recent_infect_fails.remove(p2);
          }
        }

        // checks if infection is possible
        if (((p1_infected && !p2_infected) || (!p1_infected && p2_infected)) && not_recovered && close) {
          if (!recently_failed) {
            // attempts to infect
            if (random(0, 1) < infection_chance) {
              if (p1_infected) {
                p1.infect(p2);
              }
              else p2.infect(p1);
            }
            // adds particle to recentInfectFails if infection fails
            else p1.recent_infect_fails.put(p2, 0);
          }
        }
      }
    }
  }
  // next frame for every community center
  void nextFrame() {
    collisionCheck();
    // draw community border
    noFill();

    rect(community_left, community_bottom, community_right - community_left, community_top - community_bottom);

    // draw the party centers
    for (PVector p : centers) {
      fill(255, 255, 0);
      ellipse(p.x, p.y, center_radius, center_radius);
    }

    num_currently_infected = 0;
    for (int i = 0; i < particles.size(); i ++) {
      particles.get(i).nextFrame();

      if(particles.get(i).state == 3) {
        particles.remove(i);
        continue;
      }
      else if (particles.get(i).state == 1) {
        num_currently_infected++;
      }

      // checking if the travelling particle is at its destination community
      if (particles.get(i).traveller && particles.get(i).pos.dist(particles.get(i).target_location) < 10) {
        particles.get(i).traveller = false; 
        particles.get(i).init_pos = particles.get(i).pos.get();
        
        if (particles.get(i).to_community != particles.get(i).community) {
           Community second = particles.get(i).to_community; 
           
           particles.get(i).community = second;
           second.particles.add(particles.get(i));
           particles.get(i).recent_infect_fails.clear();
           
           if (particles.get(i).partier) {
             particles.get(i).selectCenter();
           }
           
           particles.remove(i);
           i--;
        }
      }
    }

    for (Particle p : dead_particles) {
      p.nextFrame();
    }
  }
}

class Particle {
  // pos: current position
  // vel: current velocity
  // acc: current acceleration
  // init_pos: initial position of the particle, used to determine where it's quarantined
  // target center: if this person is a partier, they will go to this center
  // target_location: a place the person is trying to reach, whether it's going back to their house or to a party center
  PVector pos, vel, acc, init_pos, target_center, target_location;
  // partier: goes to a centeral location
  // gatherer: tends to group together with other gatherers and exhibits flocking behavior
  // traveller: a person who travels from one community to another
  // self_isolation: an infected person realizes it and stays in one place
  // debug: special treatment for particles that needs debugging
  boolean partier, gatherer, traveller, self_isolation, debug;
  // state: 0 is susceptible, 1 is infected, 2 is recoverd
  // infected_time: time elapsed since initially infected
  int state, infected_time, num_infected;

  // community: the community it belongs to
  // to_community: the community it is travelling to if it is a traveller
  Community community, to_community;

  // recentInfectFails: the particles which this has recently failed to infect or be infected by and the number of frames since the failed infection
  Map<Particle, Integer> recent_infect_fails;

  Particle(PVector pp, Community com) {
    community = com;
    pos = pp.get(); init_pos = pp.get();
    vel = PVector.random2D(); vel.normalize();
    vel.mult(10);
    acc = new PVector(0, 0);
    state = 0;
    infected_time = -1;
    recent_infect_fails = new HashMap<Particle, Integer>();

    // chance of a person being a partier
    if (random(0, 1) < 0.05) {
      partier = true;
      selectCenter();
    }
    else if (random(0, 1) < 0.3) gatherer = true;
  }

  void infect(Particle p) {
    p.state = 1;
    p.num_infected = 0;
    num_infected++;
  }
  
  void randomMove() {
    if(random(0, 1) > 0.02) return;
    
    Community dest = community;
    if(random(0, 1) < 0.3) {
      dest = communities[(int) random(num_communities)];
    }
    
    this.traveller = true;
    this.to_community = dest;
    this.target_location = new PVector(random(dest.community_left, dest.community_right), random(dest.community_bottom,dest.community_top));
  }

  // select a center to party at
  void selectCenter() {
      target_center = community.centers[(int) random(community.num_centers)];
      target_location = target_center;
  }
  void applyForce(PVector f) { 
    acc.add(f);
  } 
  // people don't walk in one direction forever
  void slowDown() {
    PVector f = PVector.mult(vel, 0.01);
    vel.sub(f);

    // make sure people don't gain too much velocity
    vel.limit(2);
  }
  void goTo() {
    PVector diff = PVector.sub(target_location, pos);
    diff.normalize(); diff.mult(0.05);
    vel.add(diff);
  }
  // people go to parties
  void party() {
    boolean forth = target_location == target_center;
    // people gather at a central location like a party
    if (forth && pos.dist(target_center) < 5) target_location = init_pos;
    else if (!forth && pos.dist(init_pos) < 5) target_location = target_center;
    goTo();
  }

  void bounds() {
    // bounces off of the sides
    if (pos.x - particle_radius < community.community_left || community.community_right < pos.x + particle_radius) {
      vel.x *= -1;
    }
    if (pos.y - particle_radius < community.community_bottom || community.community_top < pos.y + particle_radius) {
      vel.y *= -1;
    }

    // confined space cuz of quarantine
    if (!partier && !gatherer) {
      if (confine_radius < pos.dist(init_pos)) {
        PVector d = PVector.sub(pos, init_pos);
        d.limit(confine_radius);
        pos = PVector.add(init_pos, d);
      }
    }

    // do not glitch out of bounds, sometimes the physics gets wacky
    pos.x = max(community.community_left + particle_radius, pos.x); pos.x = min(community.community_right - particle_radius, pos.x);
    pos.y = max(community.community_bottom + particle_radius, pos.y); pos.y = min(community.community_top - particle_radius, pos.y);

  }

  // people move around randomly


  // things related to changes to the position, velocity or acceleration of the particles
  void updatePosition() {
    // people move around randomly
    randomMove();
    // if (random(0, 1) < 0.0001) println(vel);
    slowDown();
    if (partier) {
      party();
    }
    if (traveller) goTo();
    vel.add(acc);
    pos.add(vel);
    acc.mult(0);
    if (!traveller) bounds();
  }

  // this is the function being called every frame
  void nextFrame() {
    if (state != 3) {
      updatePosition();
    }
    if (state == 1) {
      if(showInfectionRadius) {
        fill(0, 0, 0, 0);
        ellipse(pos.x, pos.y, infection_radius, infection_radius);
      }
      fill(255, 0, 0);
      // there's a chance the the person will self isolate
      if (random(0, 1) < 0.01) {
        self_isolation = true;
      }
      // handling recovery {
      if (infected_time == -1) {
        infected_time = 0;
      }
      else infected_time ++;
      if (recover_time <= infected_time) {
        if(random(1) < chance_of_death) {
          state = 3;
          community.dead_particles.add(this);
        }
        else {
          state = 2;
        }

        infected_time = -1;
        community.num_recovered++;
        community.overall_infected += num_infected;
      }
      // } handling recovery
    }
    else if (state == 2) {
      fill(0, 0, 255);
    }
    else if (state == 3) {
      fill(120);
    }
    else {
      fill(255);
    }
    ellipse(pos.x, pos.y, 7, 7);
  }
}

class Slider {
  private float value;
  int leftBound, rightBound, topBound;
  float currentPos;
  float maximum;
  String thing_controlled;

  public Slider(String type, float value, int left, int right, int top, float max){
    this.value = value;
    leftBound = left + 20;
    rightBound = right - 20;
    topBound = top;
    currentPos = (value/(max/(rightBound-leftBound)))+leftBound;
    maximum = max;
    thing_controlled = type;
  }

  public float get(){
    return value;
  }

  void update(){
    stroke(0,0,255);
    strokeWeight(3);
    fill(0,0,0);
    textFont(createFont("Times New Roman",20));

    //baseline
    line(leftBound,topBound+slider_offset_y,rightBound,topBound+slider_offset_y);

    //dynamic line
    float sliderTop = topBound + slider_offset_y - slider_length_y/2;
    float sliderBottom = topBound + slider_offset_y + slider_length_y/2;

    boolean mouseInYRange = mouseY >= sliderTop + ypt - yzt && mouseY <= sliderBottom + ypt - yzt;
    boolean mouseInXRange = mouseX >= leftBound - 25 + xpt - xzt && mouseX <= rightBound + 25 + xpt - xzt;

    if(mouseInYRange && mouseInXRange && mousePressed && !panningToggle.value){
      if(mouseX > rightBound + xpt - xzt){
        mouseX = (int) (rightBound + xpt - xzt);
      }
      if(mouseX < leftBound + xpt - xzt){
        mouseX = (int) (leftBound + xpt - xzt);
      }
      currentPos = mouseX - (xpt - xzt);
    }

    line(currentPos,sliderTop,currentPos,sliderBottom);

    value = (currentPos-leftBound) * maximum/(rightBound - leftBound);
    text(thing_controlled+": "+value,leftBound,sliderTop - 10);
  }
}

boolean clicked = false;
void mouseClicked() {
  clicked = true;
}

class toggleButton{
  boolean isSidebar;
  boolean value;

  float left_bound, bottom_bound;
  float button_top, button_left;

  String on;
  String thingToggled;

  int radius;
  int transparency = 0;

  public toggleButton(String thingToggled, boolean value, int left, int bottom, boolean sidebar, int r){
    this.value = value;
    left_bound = left;
    bottom_bound = bottom;

    isSidebar = sidebar;

    button_left = left_bound + button_offset_x;
    button_top = bottom_bound + button_offset_y;

    radius = r;
    this.thingToggled = thingToggled;
  }
  
  void update(){
    stroke(0,0,255);
    strokeWeight(3);
    textSize(18);
    textAlign(CENTER);

    float top, left;
    if(isSidebar){
      top = button_top;
      left = button_left;
    }else{
      top = button_top + ypt - yzt;
      left = button_left + xpt - xzt;
    }
    boolean mouseInYRange = mouseY >= top && mouseY <= top + button_size_y;
    boolean mouseInXRange = mouseX >= left && mouseX <= left + button_size_x;

    if(mouseInYRange && mouseInXRange){
      transparency = 100;
    }else{transparency = 0;}

    if(value){
      fill(0,128,255,255-transparency);
    }else{
      fill(102,178,255,255-transparency);
    }
    rect(button_left,button_top,button_size_x,button_size_y,radius);
    
    if(mouseInYRange && mouseInXRange && clicked){
      value = !value;
      clicked = false;
    }

    fill(0,0,0);
    if(value){
      on = "On";
    }else{
      on = "Off";
    }
    text(thingToggled + ": " + on, button_left + button_size_x / 2, button_top + button_size_y / 1.4);
  }
}
void graphing(float[] graphingArray, color graphColor) {
  int num = 50;
  int everyFrame = 100;
  int goingUp = 0;
  int tempX = 600;

    for (int i = 0; i < graphingArray.length; i++) {
      int currX = ((width/2)*i/graphingArray.length+(width/2)/num)*(2/3)+600;
      int currY = (int)(height/2-graphingArray[i]+495);
    
      fill(150);

      if(i != 0){
        strokeWeight(1);
        if(i != 0 && graphingArray[i] - graphingArray[i-1] > 5){
          if(goingUp != 1){
            curveTightness(2);
            goingUp = 1;
          }
          else {
            curveTightness(1);
         }
       }
        else if(i != 0 && graphingArray[i] - graphingArray[i-1] > -5){
          goingUp = 0;
          curveTightness(0);
        }
        else{
          if(goingUp != 2){
            curveTightness(-2);
            goingUp = 2;
          }
          else{
            curveTightness(-1);
          }
        }
        stroke(graphColor, 100);
        fill(graphColor, 100);
        quad(tempX, (height/2)-graphingArray[i-1]+495, currX, currY, currX,  1245, tempX, 1245);
        strokeWeight(4);
        stroke(graphColor);
        curve(currX, (height/2)-graphingArray[i-1]+495, tempX, (height/2)-graphingArray[i-1]+495, currX, currY, currX, (height/2)-graphingArray[i-1]+495);
        strokeWeight(1);
        stroke(0);
        fill(0);
        ellipse(currX, currY, 5, 5);
        tempX = currX;
        }
  }
}
