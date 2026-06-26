package com.example.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onOnboardingComplete: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentSlide by remember { mutableStateOf(1) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    
    // Slide exit/enter animation state
    var renderedSlide by remember { mutableStateOf(1) }
    val alphaAnim = remember { Animatable(1f) }
    val translationXAnim = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    // Smooth page transitions
    fun navigateToNextSlide() {
        if (currentSlide < 5) {
            coroutineScope.launch {
                // Exit current slide
                alphaAnim.animateTo(0f, animationSpec = tween(125))
                translationXAnim.animateTo(-20f, animationSpec = tween(125))
                
                currentSlide += 1
                renderedSlide = currentSlide
                
                // Snap to starting position of incoming slide
                translationXAnim.snapTo(20f)
                
                // Enter new slide
                alphaAnim.animateTo(1f, animationSpec = tween(125))
                translationXAnim.animateTo(0f, animationSpec = tween(125))
            }
        } else {
            onOnboardingComplete(selectedCategory)
        }
    }

    fun navigateToPrevSlide() {
        if (currentSlide > 1) {
            coroutineScope.launch {
                // Exit current slide
                alphaAnim.animateTo(0f, animationSpec = tween(125))
                translationXAnim.animateTo(20f, animationSpec = tween(125))
                
                currentSlide -= 1
                renderedSlide = currentSlide
                
                // Snap to starting position of incoming slide
                translationXAnim.snapTo(-20f)
                
                // Enter new slide
                alphaAnim.animateTo(1f, animationSpec = tween(125))
                translationXAnim.animateTo(0f, animationSpec = tween(125))
            }
        }
    }

    BackHandler(enabled = currentSlide > 1) {
        navigateToPrevSlide()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = SleekBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(SleekBackground)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 100.dp) // Leave roomy space for the bottom button bar
            ) {
                // PART 2 — Progress indicator at top: 5 dots in a row, centered
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)
                ) {
                    for (i in 1..5) {
                        val isActive = renderedSlide == i
                        val dotSize by animateDpAsState(
                            targetValue = if (isActive) 8.dp else 6.dp,
                            animationSpec = tween(300),
                            label = "dot_size"
                        )
                        val dotColor by animateColorAsState(
                            targetValue = if (isActive) SleekPrimary else SleekBubblePartner,
                            animationSpec = tween(300),
                            label = "dot_color"
                        )

                        Box(
                            modifier = Modifier
                                .size(dotSize)
                                .clip(CircleShape)
                                .background(dotColor)
                                .then(
                                    if (!isActive) {
                                        Modifier.border(1.dp, SleekBorder, CircleShape)
                                    } else {
                                        Modifier
                                    }
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Current Slide wrapper with exit/enter transitions
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(alphaAnim.value)
                        .offset(x = translationXAnim.value.dp)
                        .align(Alignment.CenterHorizontally)
                ) {
                    when (renderedSlide) {
                        1 -> SlideTheHook()
                        2 -> SlideHowItWorks()
                        3 -> SlideKeyFeature()
                        4 -> SlideProgressSystem()
                        5 -> SlideFirstScenarioPicker(
                            selectedCategory = selectedCategory,
                            onSelectCategory = { selectedCategory = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1.3f))
            }

            // Bottom Area component of every slide: Fixed to bottom of screen
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp, start = 32.dp, end = 32.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Slide counter (left side, vertically centered)
                    Text(
                        text = "$renderedSlide of 5",
                        fontSize = 11.sp,
                        color = SleekTextLightGray,
                        fontWeight = FontWeight.Normal
                    )

                    // Skip intro link on Slide 5, aligned on left/bottom side
                    if (renderedSlide == 5) {
                        Text(
                            text = "Skip intro",
                            fontSize = 11.sp,
                            color = SleekTextLightGray,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier
                                .clickable { onOnboardingComplete(null) }
                                .padding(end = 16.dp)
                                .testTag("skip_intro_btn")
                        )
                    }

                    // Primary Button (right side)
                    val isNextEnabled = renderedSlide < 5 || selectedCategory != null
                    Button(
                        onClick = { navigateToNextSlide() },
                        enabled = isNextEnabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SleekPrimary,
                            disabledContainerColor = SleekPrimary.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp),
                        modifier = Modifier
                            .testTag("onboarding_primary_btn")
                            .alpha(if (isNextEnabled) 1f else 0.4f)
                    ) {
                        Text(
                            text = if (renderedSlide < 5) "Next →" else "Start practicing →",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════
// PART 3 — SLIDE 1: THE HOOK
// ════════════════════════════════════════
@Composable
fun SlideTheHook() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = androidx.compose.material.icons.Icons.Default.Info,
            contentDescription = null,
            modifier = Modifier.padding(bottom = 24.dp).size(56.dp)
        )

        Text(
            text = "Most people never practice talking to people.",
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            color = SleekTextDark,
            textAlign = TextAlign.Center,
            letterSpacing = (-0.02).sp,
            lineHeight = 31.sp,
            modifier = Modifier.widthIn(max = 400.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Athletes practice. Musicians practice. Surgeons practice. But when it comes to dating, job interviews, and hard conversations — most people just wing it.",
            fontSize = 14.sp,
            color = SleekTextGray,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            modifier = Modifier.widthIn(max = 440.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Convertible changes that.",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = SleekPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(1.dp)
                    .background(SleekPrimary.copy(alpha = 0.3f))
            )
        }
    }
}

// ════════════════════════════════════════
// PART 4 — SLIDE 2: HOW IT WORKS
// ════════════════════════════════════════
@Composable
fun SlideHowItWorks() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "HOW IT WORKS",
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = SleekTextLightGray,
            letterSpacing = 0.8.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Text(
            text = "Three steps to better conversations",
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = SleekTextDark,
            textAlign = TextAlign.Center,
            letterSpacing = (-0.02).sp,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.widthIn(max = 480.dp)
        ) {
            StepCard(
                number = "1",
                title = "Pick a scenario",
                description = "Choose from dating, work, networking, or conflict. Or describe your own situation."
            )
            StepCard(
                number = "2",
                title = "Have a real conversation",
                description = "Talk or type with an AI that reacts like a real person. No scripts. No safety net."
            )
            StepCard(
                number = "3",
                title = "Get coached",
                description = "See exactly what worked, what didn't, and one thing to do differently next time."
            )
        }
    }
}

@Composable
fun StepCard(number: String, title: String, description: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        border = BorderStroke(1.dp, SleekBorder),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(SleekPrimary.copy(alpha = 0.15f))
                    .border(1.dp, SleekPrimary.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = number,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SleekPrimary
                )
            }

            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SleekTextDark
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = SleekTextGray,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// ════════════════════════════════════════
// PART 5 — SLIDE 3: THE KEY FEATURE
// ════════════════════════════════════════
@Composable
fun SlideKeyFeature() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "SIGNATURE FEATURE",
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = SleekTextLightGray,
            letterSpacing = 0.8.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Text(
            text = "Train your judgment, not just your words",
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = SleekTextDark,
            textAlign = TextAlign.Center,
            letterSpacing = (-0.02).sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Every few turns, Convertible pauses and asks which response would actually land better.",
            fontSize = 13.sp,
            color = SleekTextGray,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .widthIn(max = 400.dp)
                .padding(bottom = 28.dp)
        )

        // Mock A/B card (the actual UI component, static/non-interactive demo)
        Card(
            colors = CardDefaults.cardColors(containerColor = SleekSurface),
            border = BorderStroke(1.dp, SleekBorder),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.widthIn(max = 460.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = SleekTextGray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Quick judgment call",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = SleekTextGray
                    )
                }

                Text(
                    text = "Which would land better right now?",
                    fontSize = 13.sp,
                    color = SleekTextDark,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // VERSION A box
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SleekBubblePartner)
                            .border(1.dp, SleekBorder, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(SleekPrimary.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "A",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SleekPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "honestly the traffic was rough but I found a new playlist so not all bad haha",
                            fontSize = 12.sp,
                            color = SleekTextDark,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Specific · Warm",
                            fontSize = 10.sp,
                            color = SleekTextLightGray,
                            fontWeight = FontWeight.Normal
                        )
                    }

                    // VERSION B box
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SleekBubblePartner)
                            .border(1.dp, SleekBorder, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(SleekBackground)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "B",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SleekTextLightGray
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "yeah it was fine, how about you?",
                            fontSize = 12.sp,
                            color = SleekTextGray,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(18.dp)) // Equalize heights a bit

                        Text(
                            text = "Generic · Flat",
                            fontSize = 10.sp,
                            color = SleekTextLightGray,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Pick one → see exactly why it works (or doesn't) · Earn XP for good judgment",
                    fontSize = 11.sp,
                    color = SleekTextLightGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// ════════════════════════════════════════
// PART 6 — SLIDE 4: PROGRESS SYSTEM
// ════════════════════════════════════════
@Composable
fun SlideProgressSystem() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "STAY MOTIVATED",
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = SleekTextLightGray,
            letterSpacing = 0.8.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Text(
            text = "Every conversation makes you better",
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = SleekTextDark,
            textAlign = TextAlign.Center,
            letterSpacing = (-0.02).sp,
            modifier = Modifier.padding(bottom = 28.dp)
        )

        Row(
            modifier = Modifier.widthIn(max = 500.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProgressCard(
                icon = "star",
                title = "Level up",
                text = "Earn XP for every session. Rise from Nervous Beginner to Convertible Master.",
                modifier = Modifier.weight(1f)
            )
            ProgressCard(
                icon = "streak",
                title = "Build streaks",
                text = "Practice daily. Even 5 minutes a day compounds fast over weeks.",
                modifier = Modifier.weight(1f)
            )
            ProgressCard(
                icon = "badge",
                title = "Earn badges",
                text = "16 badges to unlock. From First Steps to Perfect Ten.",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Level progression preview row
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(max = 500.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LevelFlowItem(name = "Nervous Beginner", active = true)
                LevelArrow()
                LevelFlowItem(name = "Smooth Talker", active = false)
                LevelArrow()
                LevelFlowItem(name = "Natural Connector", active = false)
                LevelArrow()
                LevelFlowItem(name = "Elite", active = false)
                LevelArrow()
                LevelFlowItem(name = "Master", active = false)
            }
        }
    }
}

@Composable
fun ProgressCard(icon: String, title: String, text: String, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SleekSurface),
        border = BorderStroke(1.dp, SleekBorder),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val imageVector = when (icon) {
                "star" -> androidx.compose.material.icons.Icons.Default.Star
                "streak" -> androidx.compose.material.icons.Icons.Default.Star
                "badge" -> androidx.compose.material.icons.Icons.Default.CheckCircle
                else -> androidx.compose.material.icons.Icons.Default.Info
            }
            Icon(
                imageVector = imageVector,
                contentDescription = null,
                modifier = Modifier.padding(bottom = 8.dp).size(24.dp),
                tint = SleekTextDark
            )

            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = SleekTextDark,
                modifier = Modifier.padding(bottom = 4.dp),
                textAlign = TextAlign.Center
            )

            Text(
                text = text,
                fontSize = 11.sp,
                color = SleekTextGray,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun RowScope.LevelFlowItem(name: String, active: Boolean) {
    Text(
        text = name,
        fontSize = 10.sp,
        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
        color = if (active) SleekPrimary else SleekTextLightGray,
        textAlign = TextAlign.Center,
        modifier = Modifier.weight(1f)
    )
}

@Composable
fun LevelArrow() {
    Text(
        text = "→",
        fontSize = 10.sp,
        color = SleekTextLightGray,
        modifier = Modifier.padding(horizontal = 2.dp)
    )
}

// ════════════════════════════════════════
// PART 7 — SLIDE 5: FIRST SCENARIO PICKER
// ════════════════════════════════════════
@Composable
fun SlideFirstScenarioPicker(
    selectedCategory: String?,
    onSelectCategory: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "LET'S START",
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = SleekTextLightGray,
            letterSpacing = 0.8.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Text(
            text = "What do you want to get better at?",
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = SleekTextDark,
            textAlign = TextAlign.Center,
            letterSpacing = (-0.02).sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Text(
            text = "Pick one to begin. You can try all of them later.",
            fontSize = 13.sp,
            color = SleekTextLightGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.widthIn(max = 480.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                CategoryGridCard(
                    emoji = "dating",
                    title = "Dating & Romance",
                    subtitle = "First dates, flirting, asking someone out",
                    isSelected = selectedCategory == "Dating",
                    onClick = { onSelectCategory("Dating") },
                    modifier = Modifier.weight(1f)
                )

                CategoryGridCard(
                    emoji = "work",
                    title = "Work & Career",
                    subtitle = "Interviews, salary talks, networking events",
                    isSelected = selectedCategory == "Networking",
                    onClick = { onSelectCategory("Networking") },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                CategoryGridCard(
                    emoji = "smalltalk",
                    title = "Small Talk",
                    subtitle = "Parties, strangers, making new friends",
                    isSelected = selectedCategory == "Small Talk",
                    onClick = { onSelectCategory("Small Talk") },
                    modifier = Modifier.weight(1f)
                )

                CategoryGridCard(
                    emoji = "conflict",
                    title = "Conflict Resolution",
                    subtitle = "Difficult conversations, disagreements, feedback",
                    isSelected = selectedCategory == "Conflict Resolution",
                    onClick = { onSelectCategory("Conflict Resolution") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun CategoryGridCard(
    emoji: String,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        border = BorderStroke(2.dp, if (isSelected) SleekPrimary.copy(alpha = 0.5f) else SleekBorder),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) SleekPrimaryLight.copy(alpha = 0.4f) else SleekSurface
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val imageVector = when (emoji) {
                "dating" -> androidx.compose.material.icons.Icons.Default.FavoriteBorder
                "work" -> androidx.compose.material.icons.Icons.Default.Star
                "smalltalk" -> androidx.compose.material.icons.Icons.Default.Info
                "conflict" -> androidx.compose.material.icons.Icons.Default.CheckCircle
                else -> androidx.compose.material.icons.Icons.Default.Info
            }
            Icon(
                imageVector = imageVector,
                contentDescription = null,
                modifier = Modifier.padding(bottom = 8.dp).size(28.dp),
                tint = if (isSelected) SleekPrimary else SleekTextDark
            )

            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = SleekTextDark,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = SleekTextGray,
                lineHeight = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
